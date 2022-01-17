@file:Suppress("UnstableApiUsage")

package dev.adirelle.adicrafter.crafter

import dev.adirelle.adicrafter.AdiCrafter
import dev.adirelle.adicrafter.crafter.power.PowerGenerator
import dev.adirelle.adicrafter.crafter.recipe.Grid
import dev.adirelle.adicrafter.crafter.recipe.RecipeResolver
import dev.adirelle.adicrafter.crafter.recipe.ingredient.*
import dev.adirelle.adicrafter.crafter.storage.NeighborStorageProvider
import dev.adirelle.adicrafter.crafter.storage.ResourceType
import dev.adirelle.adicrafter.crafter.storage.SingleStorageProvider
import dev.adirelle.adicrafter.crafter.storage.StorageCompoundProvider
import dev.adirelle.adicrafter.utils.Tickable
import dev.adirelle.adicrafter.utils.extensions.toBoolean
import dev.adirelle.adicrafter.utils.extensions.toInt
import dev.adirelle.adicrafter.utils.extensions.toNbt
import dev.adirelle.adicrafter.utils.extensions.toVariant
import dev.adirelle.adicrafter.utils.lazyLogger
import dev.adirelle.adicrafter.utils.toItemString
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
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext
import net.fabricmc.fabric.api.transfer.v1.transaction.base.SnapshotParticipant
import net.fabricmc.fabric.api.util.NbtType
import net.minecraft.block.BlockState
import net.minecraft.block.InventoryProvider
import net.minecraft.block.entity.BlockEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
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
    ExtendedScreenHandlerFactory,
    Tickable {

    companion object {

        private const val GRID_NBT_KEY = "Grid"
        private const val CONTENT_NBT_KEY = "Content"
        private const val FUZZY_NBT_KEY = "Fuzzy"
        private const val FLUID_NBT_KEY = "Fluid"

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
        const val FIRST_INGR_PROP_IDX = 4
        const val LAST_INGR_PROP_IDX = FIRST_INGR_PROP_IDX + GRID_SIZE - 1
        const val PROP_COUNT = LAST_INGR_PROP_IDX + 1
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

    private val ingredientFeedback = IntArray(GRID_SIZE) { 0 }
    private var forecast: ItemStack = ItemStack.EMPTY
    private var dirtyForecast = false

    private val powerGenerator = PowerGenerator.fromConfig(AdiCrafter.config.crafter.power).apply {
        onUpdate = {
            dirtyForecast = true
            markDirty()
        }
    }

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
                    SingleStorageProvider(ResourceType.POWER, powerGenerator)
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
        powerGenerator.readFromNbt(nbt)

        dirtyRecipe = true
        dirtyForecast = true
    }

    override fun writeNbt(nbt: NbtCompound) {
        super.writeNbt(nbt)

        nbt.put(GRID_NBT_KEY, grid.toNbt())
        nbt.put(CONTENT_NBT_KEY, content.toNbt())
        nbt.putBoolean(FUZZY_NBT_KEY, useFuzzyRecipe)
        nbt.putBoolean(FLUID_NBT_KEY, useFluids)
        powerGenerator.writeToNbt(nbt)
    }

    override fun tick(world: ServerWorld) {
        val recipeUpdated = updateRecipe()
        val powerUpdated = (powerGenerator as? Tickable)
            ?.let { it.tick(world); true }
            ?: false
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
        val updated = !ItemStack.areEqual(forecast, newForecast)

        if (updated) {
            forecast = newForecast
        }
        if (openScreenHandlers.isNotEmpty()) {
            updateIngredientFeedback()
        }

        return updated
    }

    private fun computeForecast(): ItemStack =
        withOuterTransaction { tx ->
            with(recipe.output) {
                val crafted = copy()
                crafted.count = storage.extract(ItemVariant.of(this), count.toLong(), tx).toInt()
                crafted
            }
        }

    private fun updateIngredientFeedback() {
        if (recipe.isEmpty || !forecast.isEmpty) {
            for (i in ingredientFeedback.indices) {
                ingredientFeedback[i] = 0
            }
            return
        }
        withOuterTransaction { tx ->
            for ((i, stack) in grid.withIndex()) {
                ingredientFeedback[i] = stack
                    .takeUnless { it.isEmpty }
                    ?.let { s -> recipe.ingredients.firstOrNull { it.matches(ItemVariant.of(s)) } }
                    ?.let { 1 - it.extractFrom(storageProvider, 1, tx).toInt() }
                    ?: 0
            }
            tx.abort()
        }
        logger.info("item feedback:")
        for ((i, value) in ingredientFeedback.withIndex()) {
            logger.info("#{}: {} -> {}", i, grid[i].item.toItemString(), value)
        }
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

    private inner class PropertyDelegateAdapter : PropertyDelegate {

        override fun size() = PROP_COUNT

        override fun get(index: Int) =
            when (index) {
                FUZZY_PROP_IDX                             ->
                    useFuzzyRecipe.toInt()
                FLUID_PROP_IDX                             ->
                    useFluids.toInt()
                POWER_PROP_IDX                             ->
                    powerGenerator.amount.toInt()
                POWER_MAX_PROP_IDX                         ->
                    powerGenerator.capacity.toInt()
                in FIRST_INGR_PROP_IDX..LAST_INGR_PROP_IDX ->
                    ingredientFeedback[index - FIRST_INGR_PROP_IDX]
                else                                       ->
                    throw IndexOutOfBoundsException("get: invalidate property delegate index: %d".format(index))
            }

        override fun set(index: Int, value: Int) {
            when (index) {
                FUZZY_PROP_IDX                             -> {
                    if (value.toBoolean() != useFuzzyRecipe) {
                        useFuzzyRecipe = value.toBoolean()
                        dirtyRecipe = true
                        markDirty()
                    }
                }
                FLUID_PROP_IDX                             -> {
                    if (value.toBoolean() != useFluids) {
                        useFluids = value.toBoolean()
                        dirtyRecipe = true
                        markDirty()
                    }
                }
                POWER_PROP_IDX                             ->
                    logger.warn("trying to set power")
                POWER_MAX_PROP_IDX                         ->
                    logger.warn("trying to set maximum power")
                in FIRST_INGR_PROP_IDX..LAST_INGR_PROP_IDX ->
                    logger.warn("trying to set ingredient feedback")
                else                                       ->
                    throw IndexOutOfBoundsException("set: invalidate property delegate index: %d".format(index))
            }
        }
    }

    private inner class IngredientFactory : RecipeResolver.IngredientFactory {

        private val fluidIngredientCache = HashMap<Item, FluidIngredient?>()

        override fun create(
            ingredients: Iterable<MinecraftIngredient>,
            grid: Iterable<ItemStack>
        ): Collection<Ingredient<*, *>> =
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

        private fun createFuzzy(stacks: Array<ItemStack>, amount: Long): ItemIngredient =
            if (stacks.size == 1)
                createExact(stacks[0], amount)
            else
                FuzzyIngredient(stacks.map { createExact(it, 1) }, amount)

        private fun createExact(stack: ItemStack, amount: Long): ItemIngredient {
            val item = createExactWithRemainder(stack, amount)
            if (useFluids) {
                findFluidIngredient(stack)?.let { fluid ->
                    return FluidSubstituteIngredient(fluid, item)
                }
            }
            return item
        }

        private fun createExactWithRemainder(stack: ItemStack, amount: Long): ItemIngredient {
            val item = ExactIngredient(stack.toVariant(), amount)
            stack.item.recipeRemainder?.let { remainder ->
                return IngredientWithRemainder(item, remainder)
            }
            return item
        }

        private fun findFluidIngredient(stack: ItemStack) =
            fluidIngredientCache.computeIfAbsent(stack.item, this::findFluidIngredientInternal)

        private fun findFluidIngredientInternal(item: Item): FluidIngredient? {
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
