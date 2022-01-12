@file:Suppress("UnstableApiUsage")

package dev.adirelle.adicrafter.crafter

import dev.adirelle.adicrafter.crafter.recipe.Grid
import dev.adirelle.adicrafter.crafter.recipe.RecipeResolver
import dev.adirelle.adicrafter.utils.extensions.toNbt
import dev.adirelle.adicrafter.utils.extensions.toVariant
import dev.adirelle.adicrafter.utils.lazyLogger
import dev.adirelle.adicrafter.utils.withOuterTransaction
import net.fabricmc.fabric.api.lookup.v1.block.BlockApiCache
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant
import net.fabricmc.fabric.api.transfer.v1.storage.Storage
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView
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
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NbtCompound
import net.minecraft.screen.NamedScreenHandlerFactory
import net.minecraft.screen.ScreenHandler
import net.minecraft.server.world.ServerWorld
import net.minecraft.text.Text
import net.minecraft.text.TranslatableText
import net.minecraft.util.ItemScatterer
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.world.WorldAccess

class CrafterBlockEntity(pos: BlockPos, state: BlockState) :
    BlockEntity(CrafterFeature.BLOCK_ENTITY_TYPE, pos, state),
    InventoryProvider,
    NamedScreenHandlerFactory {

    companion object {

        private const val GRID_NBT_KEY = "Grid"
        private const val CONTENT_NBT_KEY = "Content"
        private const val FUZZY_NBT_KEY = "Fuzzy"

        const val GRID_SIZE = Grid.SIZE
        const val GRID_WIDTH = Grid.WIDTH
        const val GRID_HEIGHT = Grid.HEIGHT

        const val OUTPUT_SLOT = GRID_SIZE
        const val RESULT_SLOT = OUTPUT_SLOT + 1
        const val CONTENT_SLOT = RESULT_SLOT + 1
        const val INVENTORY_SIZE = CONTENT_SLOT + 1
    }

    private val logger by lazyLogger

    val inventory = InventoryAdapter()
    val storage = StorageAdapter()

    private var grid = Grid.empty()
    private var fuzzyFlag: Boolean = false

    private var recipe: Recipe = Recipe.EMPTY
    private var dirtyRecipe = false

    private var content: ItemStack = ItemStack.EMPTY

    private var forecast: ItemStack = ItemStack.EMPTY
    private var dirtyForecast = false

    private val openScreenHandlers = ArrayList<ScreenHandler>(2)

    override fun getInventory(state: BlockState, world: WorldAccess, pos: BlockPos): SidedInventory =
        inventory

    override fun getDisplayName(): Text = TranslatableText("block.adicrafter.crafter")

    override fun createMenu(syncId: Int, playerInventory: PlayerInventory, player: PlayerEntity): ScreenHandler {
        val handler = CrafterScreenHandler(syncId, playerInventory, this.inventory, this)
        openScreenHandlers.add(handler)
        return handler
    }

    fun onScreenHandlerClosed(handler: ScreenHandler) {
        openScreenHandlers.remove(handler)
    }

    override fun readNbt(nbt: NbtCompound) {
        super.readNbt(nbt)

        grid = Grid.fromNbt(nbt.getList(GRID_NBT_KEY, NbtType.COMPOUND))
        content = ItemStack.fromNbt(nbt.getCompound(CONTENT_NBT_KEY))
        fuzzyFlag = nbt.getBoolean(FUZZY_NBT_KEY)

        dirtyRecipe = true
        dirtyForecast = true
    }

    override fun writeNbt(nbt: NbtCompound) {
        super.writeNbt(nbt)

        nbt.put(GRID_NBT_KEY, grid.toNbt())
        nbt.put(CONTENT_NBT_KEY, content.toNbt())
        nbt.putBoolean(FUZZY_NBT_KEY, fuzzyFlag)
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
        (world as? ServerWorld)?.let { world ->
            dirtyRecipe = false
            val newRecipe = RecipeResolver.of(world).resolve(grid, fuzzyFlag)
            logger.debug("recipe resolved to: {}", newRecipe)
            if (newRecipe != recipe) {
                recipe = newRecipe
                if (!content.isOf(recipe.output.item)) {
                    dropContent()
                }
                dirtyForecast = true
                return true
            }
        }
        return false
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

    private val apiCaches: Map<Direction, BlockApiCache<Storage<ItemVariant>, Direction>> by lazy {
        (world as? ServerWorld)?.let { world ->
            buildMap {
                for (direction in Direction.values()) {
                    put(direction, BlockApiCache.create(ItemStorage.SIDED, world, pos.offset(direction)))
                }
            }
        } ?: mapOf()
    }

    private fun findStorage(): Storage<ItemVariant> =
        CombinedStorage(
            apiCaches.entries.mapNotNull { (direction, cache) -> cache.find(direction) }
        )

    inner class StorageAdapter : SingleSlotStorage<ItemVariant>, SnapshotParticipant<ItemStack>() {

        override fun isResourceBlank() = recipe.isEmpty
        override fun getCapacity() = resource.item.maxCount.toLong()
        override fun getResource() = recipe.output.toVariant()
        override fun getAmount() = content.count.toLong()
        override fun supportsInsertion() = false
        override fun insert(resource: ItemVariant, maxAmount: Long, tx: TransactionContext) = 0L
        override fun exactView(transaction: TransactionContext, resource: ItemVariant): StorageView<ItemVariant> = this

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
            RecipeCrafter(recipe, findStorage())
                .extract(recipe.output.toVariant(), amount.toLong(), tx).toInt()

        override fun createSnapshot(): ItemStack = content.copy()
        override fun readSnapshot(snapshot: ItemStack) {
            content = snapshot
        }

        override fun onFinalCommit() {
            dirtyForecast = true
            markDirty()
        }
    }

    inner class InventoryAdapter : SidedInventory {

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
            slot < grid.size

        override fun clear() {
            for (i in 0 until grid.size) {
                grid[i] = ItemStack.EMPTY
            }
            dirtyRecipe = true
            this@CrafterBlockEntity.markDirty()
        }

        override fun getStack(slot: Int): ItemStack =
            when (slot) {
                in 0 until GRID_SIZE -> grid[slot]
                OUTPUT_SLOT          -> forecast
                RESULT_SLOT          -> recipe.output
                CONTENT_SLOT         -> content
                else                 -> ItemStack.EMPTY
            }.copy()

        override fun removeStack(slot: Int, amount: Int): ItemStack =
            when (slot) {
                in 0 until GRID_SIZE -> {
                    grid[slot] = ItemStack.EMPTY
                    dirtyRecipe = true
                    this@CrafterBlockEntity.markDirty()
                    ItemStack.EMPTY
                }
                OUTPUT_SLOT          -> {
                    withOuterTransaction { tx ->
                        val extracted = storage.extract(recipe.output.toVariant(), amount.toLong(), tx)
                        recipe.output.copy().apply { count = extracted.toInt() }
                    }
                }
                else                 -> ItemStack.EMPTY
            }

        override fun removeStack(slot: Int): ItemStack =
            removeStack(slot, getStack(slot).count)

        override fun setStack(slot: Int, stack: ItemStack) {
            if (slot in 0 until GRID_SIZE) {
                grid[slot] = stack.copy().apply { count = 1 }
                dirtyRecipe = true
                this@CrafterBlockEntity.markDirty()
            }
        }

        override fun canInsert(slot: Int, stack: ItemStack, dir: Direction?) =
            slot < GRID_SIZE

        override fun canExtract(slot: Int, stack: ItemStack, dir: Direction?) =
            slot == OUTPUT_SLOT && stack.isOf(recipe.output.item)
    }
}
