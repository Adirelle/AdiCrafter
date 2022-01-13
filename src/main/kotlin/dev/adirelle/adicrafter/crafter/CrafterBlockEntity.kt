@file:Suppress("UnstableApiUsage")

package dev.adirelle.adicrafter.crafter

import dev.adirelle.adicrafter.crafter.recipe.Grid
import dev.adirelle.adicrafter.crafter.recipe.RecipeResolver
import dev.adirelle.adicrafter.crafter.recipe.ingredient.*
import dev.adirelle.adicrafter.utils.extensions.toBoolean
import dev.adirelle.adicrafter.utils.extensions.toInt
import dev.adirelle.adicrafter.utils.extensions.toNbt
import dev.adirelle.adicrafter.utils.extensions.toVariant
import dev.adirelle.adicrafter.utils.lazyLogger
import dev.adirelle.adicrafter.utils.withOuterTransaction
import io.github.cottonmc.cotton.gui.PropertyDelegateHolder
import net.fabricmc.fabric.api.lookup.v1.block.BlockApiCache
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory
import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant
import net.fabricmc.fabric.api.transfer.v1.storage.Storage
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView
import net.fabricmc.fabric.api.transfer.v1.storage.TransferVariant
import net.fabricmc.fabric.api.transfer.v1.storage.base.CombinedStorage
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
    ExtendedScreenHandlerFactory {

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
        const val PROP_COUNT = 2
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

    private var forecast: ItemStack = ItemStack.EMPTY
    private var dirtyForecast = false

    private val openScreenHandlers = ArrayList<ScreenHandler>(2)

    private val ingredientFactory = IngredientFactory()
    private val inventory = InventoryAdapter()
    private val propertyDelegate = PropertyDelegateAdapter()

    private val storageProvider by lazy {
        (world as? ServerWorld?)
            ?.let { NeighboringStorageProvider(it, pos) }
            ?: NoStorageProvider()
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

        dirtyRecipe = true
        dirtyForecast = true
    }

    override fun writeNbt(nbt: NbtCompound) {
        super.writeNbt(nbt)

        nbt.put(GRID_NBT_KEY, grid.toNbt())
        nbt.put(CONTENT_NBT_KEY, content.toNbt())
        nbt.putBoolean(FUZZY_NBT_KEY, useFuzzyRecipe)
        nbt.putBoolean(FLUID_NBT_KEY, useFluids)
    }

    fun tick() {
        val recipeUpdated = updateRecipe()
        val forecastUpdated = updateForecast()
        if (recipeUpdated || forecastUpdated) {
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
        logger.info("recipe: {}, ingredients: {}", recipe.id, recipe.ingredients.joinToString())

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

    private inner class StorageAdapter : SingleSlotStorage<ItemVariant>, SnapshotParticipant<ItemStack>() {

        override fun isResourceBlank() = recipe.isEmpty
        override fun getCapacity() = resource.item.maxCount.toLong()
        override fun getResource() = recipe.output.toVariant()
        override fun getAmount() = content.count.toLong()
        override fun supportsInsertion() = false
        override fun insert(resource: ItemVariant, maxAmount: Long, tx: TransactionContext) = 0L
        override fun exactView(transaction: TransactionContext, resource: ItemVariant): StorageView<ItemVariant> =
            this

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
                FUZZY_PROP_IDX -> useFuzzyRecipe.toInt()
                FLUID_PROP_IDX -> useFluids.toInt()
                else           -> throw IndexOutOfBoundsException()
            }

        override fun set(index: Int, value: Int) {
            when (index) {
                FUZZY_PROP_IDX -> {
                    if (value.toBoolean() != useFuzzyRecipe) {
                        useFuzzyRecipe = value.toBoolean()
                        dirtyRecipe = true
                        markDirty()
                    }
                }
                FLUID_PROP_IDX -> {
                    if (value.toBoolean() != useFluids) {
                        useFluids = value.toBoolean()
                        dirtyRecipe = true
                        markDirty()
                    }
                }
                else           ->
                    throw IndexOutOfBoundsException()
            }
        }
    }

    private class NoStorageProvider : StorageProvider {

        override fun <T : TransferVariant<*>> getStorage(resourceType: ResourceType<T>): Storage<T> {
            throw RuntimeException("should not be used on client")
        }
    }

    private class NeighboringStorageProvider(
        world: ServerWorld,
        pos: BlockPos
    ) : StorageProvider {

        private val logger by lazyLogger

        private val caches = buildMap {
            for (resourceType in listOf(ResourceType.ITEM, ResourceType.FLUID)) {
                put(resourceType, buildMap {
                    for (direction in Direction.values()) {
                        put(direction, BlockApiCache.create(resourceType.storageLookup, world, pos.offset(direction)))
                    }
                })
            }
        }

        override fun <T : TransferVariant<*>> getStorage(resourceType: ResourceType<T>): Storage<T> {
            val caches = caches[resourceType]
                ?: throw RuntimeException("unsupported resource type: %s".format(resourceType))

            @Suppress("UNCHECKED_CAST")
            val storages = caches.entries.mapNotNull { (direction, cache) ->
                cache.find(direction)
            } as List<Storage<T>>
            logger.info("found {} storages: {}", resourceType, storages)

            return CombinedStorage(storages)
        }
    }

    private inner class IngredientFactory : RecipeResolver.IngredientFactory {

        private val fluidIngredientCache = HashMap<Item, ExactIngredient<FluidVariant>?>()

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

        private fun createFuzzy(stacks: Array<ItemStack>, amount: Long): Ingredient<ItemVariant> =
            if (stacks.size == 1)
                createExact(stacks[0], amount)
            else
                FuzzyIngredient(stacks.map { createExact(it, amount) })

        private fun createExact(stack: ItemStack, amount: Long): Ingredient<ItemVariant> {
            val item = createExactWithRemainder(stack, amount)
            if (useFluids) {
                findFluidIngredient(stack)?.let { fluid ->
                    return FluidSubstituteIngredient(fluid, item)
                }
            }
            return item
        }

        private fun createExactWithRemainder(stack: ItemStack, amount: Long): Ingredient<ItemVariant> {
            val item = ExactIngredient(stack.toVariant(), amount)
            stack.item.recipeRemainder?.let { remainder ->
                return IngredientWithRemainder(item, ItemVariant.of(remainder))
            }
            return item
        }

        private fun findFluidIngredient(stack: ItemStack) =
            fluidIngredientCache.computeIfAbsent(stack.item, this::findFluidIngredientInternal)

        private fun findFluidIngredientInternal(item: Item): ExactIngredient<FluidVariant>? {
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
