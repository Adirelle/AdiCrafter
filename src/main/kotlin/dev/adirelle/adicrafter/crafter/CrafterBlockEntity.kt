@file:Suppress("UnstableApiUsage")

package dev.adirelle.adicrafter.crafter

import dev.adirelle.adicrafter.crafter.power.PowerVariant
import dev.adirelle.adicrafter.crafter.power.PowerVariant.INSTANCE
import dev.adirelle.adicrafter.crafter.recipe.Grid
import dev.adirelle.adicrafter.crafter.recipe.RecipeResolver
import dev.adirelle.adicrafter.crafter.recipe.ingredient.*
import dev.adirelle.adicrafter.crafter.storage.NeighborStorageProvider
import dev.adirelle.adicrafter.crafter.storage.ResourceType
import dev.adirelle.adicrafter.crafter.storage.SingleStorageProvider
import dev.adirelle.adicrafter.crafter.storage.StorageCompoundProvider
import dev.adirelle.adicrafter.utils.extensions.toBoolean
import dev.adirelle.adicrafter.utils.extensions.toInt
import dev.adirelle.adicrafter.utils.extensions.toNbt
import dev.adirelle.adicrafter.utils.extensions.toVariant
import dev.adirelle.adicrafter.utils.lazyLogger
import dev.adirelle.adicrafter.utils.withOuterTransaction
import io.github.cottonmc.cotton.gui.PropertyDelegateHolder
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory
import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant
import net.fabricmc.fabric.api.transfer.v1.storage.Storage
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleVariantStorage
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext
import net.fabricmc.fabric.api.transfer.v1.transaction.base.SnapshotParticipant
import net.fabricmc.fabric.api.util.NbtType
import net.minecraft.block.BlockState
import net.minecraft.block.InventoryProvider
import net.minecraft.block.entity.BlockEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.fluid.Fluid
import net.minecraft.inventory.SidedInventory
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NbtCompound
import net.minecraft.network.PacketByteBuf
import net.minecraft.screen.PropertyDelegate
import net.minecraft.screen.ScreenHandler
import net.minecraft.screen.ScreenHandlerContext
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.server.world.ServerWorld
import net.minecraft.text.Text
import net.minecraft.text.TranslatableText
import net.minecraft.util.ItemScatterer
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.world.WorldAccess
import net.minecraft.recipe.Ingredient as MinecraftIngredient

class CrafterBlockEntity(pos: BlockPos, state: BlockState) :
    BlockEntity(CrafterFeature.BLOCK_ENTITY_TYPE, pos, state),
    InventoryProvider,
    PropertyDelegateHolder,
    ExtendedScreenHandlerFactory {

    companion object {

        private const val GRID_NBT_KEY = "Grid"
        private const val CONTENT_NBT_KEY = "Content"
        private const val FUZZY_NBT_KEY = "Fuzzy"
        private const val FLUID_NBT_KEY = "Fluid"
        private const val POWER_NBT_KEY = "Power"

        const val GRID_SIZE = Grid.SIZE
        const val GRID_WIDTH = Grid.WIDTH
        const val GRID_HEIGHT = Grid.HEIGHT

        const val GRID_FIRST_SLOT = 0
        const val GRID_LAST_SLOT = GRID_SIZE - 1
        const val OUTPUT_SLOT = GRID_SIZE
        const val RESULT_SLOT = OUTPUT_SLOT + 1
        const val CONTENT_SLOT = RESULT_SLOT + 1
        const val INVENTORY_SIZE = CONTENT_SLOT + 1

        val GRID_SLOTS = GRID_FIRST_SLOT..GRID_LAST_SLOT

        const val FUZZY_PROP_IDX = 0
        const val FLUID_PROP_IDX = 1
        const val POWER_PROP_IDX = 2
        const val POWER_MAX_PROP_IDX = 3
        const val PROP_COUNT = 4
    }

    private val logger by lazyLogger

    val storage: Storage<ItemVariant> = StorageAdapter()

    private var grid = Grid.empty()

    private var useFuzzyRecipe: Boolean = false
    private var useFluids: Boolean = false

    private var recipe: Recipe = Recipe.EMPTY
    private var dirtyRecipe = false
    private var crafter: StorageView<ItemVariant> = RecipeCrafter.EMPTY

    private var content: ItemStack = ItemStack.EMPTY

    private val powerStorage = PowerStorage(1000L)

    private var forecast: ItemStack = ItemStack.EMPTY
    private var dirtyForecast = false

    private val openScreenHandlers = ArrayList<ScreenHandler>(2)

    private val ingredientFactory = IngredientFactory()
    private val inventory = InventoryAdapter()
    private val propertyDelegate = PropertyDelegateAdapter()

    private val storageProvider by lazy {
        (world as? ServerWorld?)
            ?.let { world ->
                StorageCompoundProvider.of(
                    NeighborStorageProvider(ResourceType.ITEM, ItemStorage.SIDED, world, pos),
                    NeighborStorageProvider(ResourceType.FLUID, FluidStorage.SIDED, world, pos),
                    SingleStorageProvider(ResourceType.POWER, powerStorage)
                )
            }
            ?: StorageCompoundProvider.of()
    }

    override fun getInventory(state: BlockState, world: WorldAccess, pos: BlockPos): SidedInventory =
        inventory

    override fun getPropertyDelegate(): PropertyDelegate =
        propertyDelegate

    override fun getDisplayName(): Text = TranslatableText("block.adicrafter.crafter")

    override fun createMenu(syncId: Int, playerInventory: PlayerInventory, player: PlayerEntity): ScreenHandler {
        val handler = CrafterScreenHandler(syncId, playerInventory, ScreenHandlerContext.create(world, pos))
        openScreenHandlers.add(handler)
        return handler
    }

    override fun writeScreenOpeningData(player: ServerPlayerEntity, buf: PacketByteBuf) {
        buf.writeBoolean(useFuzzyRecipe)
        buf.writeBoolean(useFluids)
    }

    fun onScreenHandlerClosed(handler: ScreenHandler) {
        openScreenHandlers.remove(handler)
    }

    override fun readNbt(nbt: NbtCompound) {
        super.readNbt(nbt)

        grid = Grid.fromNbt(nbt.getList(GRID_NBT_KEY, NbtType.COMPOUND))
        content = ItemStack.fromNbt(nbt.getCompound(CONTENT_NBT_KEY))
        useFuzzyRecipe = nbt.getBoolean(FUZZY_NBT_KEY)
        useFluids = nbt.getBoolean(FLUID_NBT_KEY)
        powerStorage.amount = nbt.getLong(POWER_NBT_KEY)

        dirtyRecipe = true
        dirtyForecast = true
    }

    override fun writeNbt(nbt: NbtCompound) {
        super.writeNbt(nbt)

        nbt.put(GRID_NBT_KEY, grid.toNbt())
        nbt.put(CONTENT_NBT_KEY, content.toNbt())
        nbt.putBoolean(FUZZY_NBT_KEY, useFuzzyRecipe)
        nbt.putBoolean(FLUID_NBT_KEY, useFluids)
        nbt.putLong(POWER_NBT_KEY, powerStorage.amount)
    }

    fun tick() {
        val recipeUpdated = updateRecipe()
        val powerUpdated = updatePower()
        val forecastUpdated = updateForecast()
        if (recipeUpdated || powerUpdated || forecastUpdated) {
            notifyScreenHandlers()
        }
    }

    fun dropContent() {
        if (content.isEmpty) return
        world?.let { world ->
            pos.up().let { dropPos ->
                ItemScatterer.spawn(
                    world,
                    dropPos.x.toDouble(),
                    dropPos.y.toDouble(),
                    dropPos.z.toDouble(),
                    content
                )
            }
            content.count = 0
        }
    }

    private fun notifyScreenHandlers() {
        openScreenHandlers.forEach { it.sendContentUpdates() }
    }

    private fun updateRecipe(): Boolean {
        if (!dirtyRecipe) return false
        val world = world as? ServerWorld ?: return false
        dirtyRecipe = false

        recipe = RecipeResolver.of(world).resolve(grid, ingredientFactory)
        logger.debug("recipe: {}, ingredients: {}", recipe.id, recipe.ingredients.joinToString())

        if (!content.isOf(recipe.output.item)) {
            dropContent()
        }

        crafter =
            if (recipe.isEmpty) RecipeCrafter.EMPTY
            else RecipeCrafter(recipe, storageProvider)

        dirtyForecast = true
        return true
    }

    private fun updateForecast(): Boolean {
        if (!dirtyForecast) return false
        dirtyForecast = false

        val newForecast = computeForecast()
        if (ItemStack.areEqual(forecast, newForecast)) return false

        forecast = newForecast
        return true
    }

    private fun computeForecast(): ItemStack =
        withOuterTransaction { tx ->
            with(recipe.output) {
                val crafted = copy()
                crafted.count = storage.extract(ItemVariant.of(this), count.toLong(), tx).toInt()
                crafted
            }
        }

    private fun updatePower() =
        withOuterTransaction { tx ->
            val inserted = powerStorage.insert(INSTANCE, 10L, tx)
            tx.commit()
            inserted > 0
        }

    private inner class StorageAdapter : SingleSlotStorage<ItemVariant>, SnapshotParticipant<ItemStack>() {

        override fun isResourceBlank() = recipe.isEmpty
        override fun getCapacity() = recipe.output.count.toLong()
        override fun getResource() = recipe.output.toVariant()
        override fun getAmount() = forecast.count.toLong()
        override fun supportsInsertion() = false
        override fun insert(resource: ItemVariant, maxAmount: Long, tx: TransactionContext) = 0L
        override fun exactView(transaction: TransactionContext, resource: ItemVariant): StorageView<ItemVariant>? =
            if (resource.isOf(recipe.output.item)) this
            else null

        override fun extract(resource: ItemVariant, maxAmount: Long, tx: TransactionContext): Long {
            if (maxAmount < 1 || recipe.isEmpty || !resource.matches(recipe.output)) return 0L
            updateSnapshots(tx)
            val maxAmountInt = maxAmount.toInt()
            val current = content.count
            if (maxAmountInt > current) {
                val crafted = craft(maxAmountInt - current, tx)
                content = recipe.output.copy().apply {
                    count = current + crafted
                }
            }
            val extracted = content.split(maxAmountInt)
            return extracted.count.toLong()
        }

        private fun craft(amount: Int, tx: TransactionContext) =
            crafter.extract(recipe.output.toVariant(), amount.toLong(), tx).toInt()

        override fun createSnapshot(): ItemStack = content.copy()
        override fun readSnapshot(snapshot: ItemStack) {
            content = snapshot
        }

        override fun onFinalCommit() {
            dirtyForecast = true
            markDirty()
        }
    }

    private inner class InventoryAdapter : SidedInventory {

        override fun size() = INVENTORY_SIZE
        override fun isEmpty() = false
        override fun markDirty() {}
        override fun getMaxCountPerStack() = 1
        override fun canPlayerUse(player: PlayerEntity?) = true
        override fun getAvailableSlots(side: Direction?) = intArrayOf(OUTPUT_SLOT)

        override fun onOpen(player: PlayerEntity?) {
            super.onOpen(player)
            dirtyForecast = true
        }

        override fun isValid(slot: Int, stack: ItemStack) =
            slot in GRID_SLOTS

        override fun clear() {
            for (i in GRID_SLOTS) {
                grid[i] = ItemStack.EMPTY
            }
            dirtyRecipe = true
            this@CrafterBlockEntity.markDirty()
        }

        override fun getStack(slot: Int): ItemStack =
            when (slot) {
                in GRID_SLOTS -> grid[slot]
                OUTPUT_SLOT   -> forecast
                RESULT_SLOT   -> recipe.output
                CONTENT_SLOT  -> content
                else          -> ItemStack.EMPTY
            }.copy()

        override fun removeStack(slot: Int, amount: Int): ItemStack =
            when (slot) {
                in GRID_SLOTS -> {
                    grid[slot] = ItemStack.EMPTY
                    dirtyRecipe = true
                    this@CrafterBlockEntity.markDirty()
                    ItemStack.EMPTY
                }
                OUTPUT_SLOT   -> {
                    withOuterTransaction { tx ->
                        val extracted = storage.extract(recipe.output.toVariant(), amount.toLong(), tx)
                        recipe.output.copy().apply { count = extracted.toInt() }
                    }
                }
                else          ->
                    ItemStack.EMPTY
            }

        override fun removeStack(slot: Int): ItemStack =
            removeStack(slot, getStack(slot).count)

        override fun setStack(slot: Int, stack: ItemStack) {
            if (slot in GRID_SLOTS) {
                grid[slot] = stack.copy().apply { count = 1 }
                dirtyRecipe = true
                this@CrafterBlockEntity.markDirty()
            }
        }

        override fun canInsert(slot: Int, stack: ItemStack, dir: Direction?) =
            slot in GRID_SLOTS

        override fun canExtract(slot: Int, stack: ItemStack, dir: Direction?) =
            slot == OUTPUT_SLOT && stack.isOf(recipe.output.item)
    }

    private inner class PowerStorage(val innerCapacity: Long) : SingleVariantStorage<PowerVariant>() {

        override fun onFinalCommit() {
            dirtyForecast = true
            markDirty()
        }

        override fun getCapacity(variant: PowerVariant) = innerCapacity
        override fun getBlankVariant() = INSTANCE
    }

    private inner class PropertyDelegateAdapter : PropertyDelegate {

        override fun size() = PROP_COUNT

        override fun get(index: Int) =
            when (index) {
                FUZZY_PROP_IDX     -> useFuzzyRecipe.toInt()
                FLUID_PROP_IDX     -> useFluids.toInt()
                POWER_PROP_IDX     -> powerStorage.amount.toInt()
                POWER_MAX_PROP_IDX -> powerStorage.capacity.toInt()
                else               -> throw IndexOutOfBoundsException()
            }

        override fun set(index: Int, value: Int) {
            when (index) {
                FUZZY_PROP_IDX     -> {
                    if (value.toBoolean() != useFuzzyRecipe) {
                        useFuzzyRecipe = value.toBoolean()
                        dirtyRecipe = true
                        markDirty()
                    }
                }
                FLUID_PROP_IDX     -> {
                    if (value.toBoolean() != useFluids) {
                        useFluids = value.toBoolean()
                        dirtyRecipe = true
                        markDirty()
                    }
                }
                POWER_PROP_IDX     -> logger.warn("trying to set power")
                POWER_MAX_PROP_IDX -> logger.warn("trying to set maximum power")
                else               ->
                    throw IndexOutOfBoundsException()
            }
        }
    }

    private inner class IngredientFactory : RecipeResolver.IngredientFactory {

        private val fluidIngredientCache = HashMap<Item, ExactIngredient<Fluid>?>()

        override fun create(
            ingredients: Iterable<MinecraftIngredient>,
            grid: Iterable<ItemStack>
        ): Collection<Ingredient<*>> =
            if (useFuzzyRecipe)
                ingredients
                    .filterNot { it.isEmpty }
                    .groupBy { it }
                    .map { (ingredient, list) -> createFuzzy(ingredient.matchingStacks, list.size.toLong()) }
            else
                grid
                    .filterNot { it.isEmpty }
                    .groupBy { it }
                    .map { (item, stacks) -> createExact(item, stacks.size.toLong()) }

        private fun createFuzzy(stacks: Array<ItemStack>, amount: Long): Ingredient<Item> =
            if (stacks.size == 1)
                createExact(stacks[0], amount)
            else
                FuzzyIngredient(stacks.map { createExact(it, 1) }, amount)

        private fun createExact(stack: ItemStack, amount: Long): Ingredient<Item> {
            val item = createExactWithRemainder(stack, amount)
            if (useFluids) {
                findFluidIngredient(stack)?.let { fluid ->
                    return FluidSubstituteIngredient(fluid, item)
                }
            }
            return item
        }

        private fun createExactWithRemainder(stack: ItemStack, amount: Long): Ingredient<Item> {
            val item = ExactIngredient(stack.toVariant(), amount)
            stack.item.recipeRemainder?.let { remainder ->
                return IngredientWithRemainder(item, remainder)
            }
            return item
        }

        private fun findFluidIngredient(stack: ItemStack) =
            fluidIngredientCache.computeIfAbsent(stack.item, this::findFluidIngredientInternal)

        private fun findFluidIngredientInternal(item: Item): ExactIngredient<Fluid>? {
            val context = ContainerItemContext.withInitial(ItemStack(item, 1))
            val storage = context.find(FluidStorage.ITEM) ?: return null
            return withOuterTransaction { tx ->
                storage.iterable(tx)
                    .singleOrNull { !it.isResourceBlank }
                    ?.let { ExactIngredient(it.resource, it.amount) }
            }
        }
    }
}
