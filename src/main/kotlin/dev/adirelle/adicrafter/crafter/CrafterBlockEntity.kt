package dev.adirelle.adicrafter.crafter

import dev.adirelle.adicrafter.utils.*
import dev.adirelle.adicrafter.utils.extension.*
import net.minecraft.block.BlockState
import net.minecraft.block.entity.BlockEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NbtCompound
import net.minecraft.recipe.CraftingRecipe
import net.minecraft.screen.NamedScreenHandlerFactory
import net.minecraft.text.Text
import net.minecraft.text.TranslatableText
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import java.util.*

@Suppress("UnstableApiUsage")
class CrafterBlockEntity(pos: BlockPos, state: BlockState) :
    BlockEntity(Crafter.BLOCK_ENTITY_TYPE, pos, state),
    NamedScreenHandlerFactory {

    companion object {

        private const val GRID_NBT_KEY = "Grid"
        private const val BUFFER_NBT_KEY = "Buffer"

        const val GRID_WIDTH = 3
        const val GRID_HEIGHT = 3
        const val GRID_SIZE = GRID_WIDTH * GRID_HEIGHT
    }

    private val logger by lazyLogger()

    private var dirtyRecipe = false
    private var dirtyForecast = false

    private var grid = MutableList(GRID_SIZE) { ItemStack.EMPTY }
    private var recipe = Optional.empty<CraftingRecipe>()
    private var buffer: ItemStack = ItemStack.EMPTY
    private var forecast: ItemStack = ItemStack.EMPTY

    data class DisplayState(
        val grid: List<ItemStack>,
        val recipe: Optional<CraftingRecipe>,
        val buffer: ItemStack,
        val forecast: ItemStack
    )

    val displayBroadcaster = Broadcaster<DisplayState>()
    fun getDisplayState() = DisplayState(grid, recipe, buffer, forecast)

    fun tick(world: World) {
        var shouldBroadcast = dirtyRecipe
        if (dirtyRecipe) {
            updateRecipe(world)
        }
        if (dirtyForecast && displayBroadcaster.hasListeners()) {
            shouldBroadcast = true
        }
        if (shouldBroadcast) {
            displayBroadcaster.emit(getDisplayState())
        }
    }

    fun setGrid(newGrid: List<ItemStack>) =
        ifDifferent(grid, newGrid) {
            expectSameSizeAs(newGrid, grid).forEachIndexed { index, stack ->
                grid[index] = stack.copy()
            }
            dirtyRecipe = true
            markDirty()
        }

    private fun updateRecipe(world: World) {
        val newRecipe = CraftingRecipeResolver.of(world).resolve(grid)
        if (!areEqual(newRecipe, recipe)) {
            recipe = newRecipe
            dirtyForecast = true
        }
        dirtyRecipe = false
    }

    override fun getDisplayName(): Text = TranslatableText("block.adicrafter.crafter")

    override fun createMenu(syncId: Int, playerInventory: PlayerInventory, player: PlayerEntity) =
        CrafterScreenHandler(syncId, playerInventory, this)

    override fun readNbt(nbt: NbtCompound) {
        super.readNbt(nbt)

        val newBuffer = nbt.getItemStack(BUFFER_NBT_KEY)
        if (!areEqual(buffer, newBuffer)) {
            logger.info("read from NBT: {}, {}", newBuffer)
            buffer = newBuffer
            dirtyForecast = true
        }

        val newGrid = nbt.getItemStacks(GRID_NBT_KEY)
        if (!areEqual(grid, newGrid)) {
            logger.info("read from NBT: {}, {}", toItemString(newGrid))
            grid = ArrayList(newGrid.map { it.copy() })
            dirtyRecipe = true
        }
    }

    override fun writeNbt(nbt: NbtCompound) {
        super.writeNbt(nbt)

        nbt.putItemStacks(GRID_NBT_KEY, grid)
        nbt.putItemStack(BUFFER_NBT_KEY, buffer)

        logger.info("written to NBT: {}, {}", buffer, toItemString(grid))
    }

/*
    private fun updateForecast(txc: TransactionContext? = null) {
        if (!invalidForecast || (Transaction.isOpen() && txc == null)) return
        invalidForecast = false
        val newForecast = inNestedTransaction(txc) { tx ->
            Crafter(tx).available().also {
                tx.abort()
            }
        }
        if (!ItemStack.areEqual(newForecast, forecast)) {
            logger.info("forecast changed: {} -> {}", forecast, newForecast)
            forecast = newForecast
            outputUpdateEmitter.emit(OutputUpdate(buffer, forecast))
        }
    }

    data class GridUpdate(val grid: Array<ItemStack>)

    data class RecipeUpdate(val grid: Array<ItemStack>, val result: ItemStack)

    data class OutputUpdate(val buffer: ItemStack, val output: ItemStack)

    private inner class Crafter(val tx: Transaction) : SnapshotParticipant<ItemStack>() {

        private val logger by lazyLogger()

        fun available(): ItemStack =
            if (buffer.isEmpty) craft()
            else buffer.copy()

        fun take(maxAmount: Int): ItemStack {
            updateSnapshots(tx)
            logger.info("request for {} items", maxAmount)
            if (buffer.isEmpty) {
                buffer = craft()
            }
            val extracted = buffer.split(maxAmount)
            logger.info("taking {}s, {}s left", extracted, maxAmount, buffer)
            return extracted
        }

        private fun craft(): ItemStack {
            val crafted = recipeOutput.copy()
            logger.info("crafted {}s", crafted)
            return crafted
        }

        override fun createSnapshot(): ItemStack = buffer.copy()
        override fun readSnapshot(snapshot: ItemStack) {
            logger.info("reverting changes")
            buffer = snapshot
        }
    }

    private inner class StorageImpl : SingleSlotStorage<ItemVariant> {

        private val logger by lazyLogger()

        override fun extract(resource: ItemVariant, maxAmount: Long, txc: TransactionContext?) =
            if (!resource.canCombineWith(this.resource)) {
                logger.info("cannot stack resource: {}", resource)
                0L
            } else
                inNestedTransaction(txc) { tx ->
                    logger.info("request for up to {} {}s", maxAmount, resource.item)
                    val crafter = Crafter(tx)
                    var remaining = maxAmount
                    do {
                        val crafted = crafter.take(remaining.toInt())
                        remaining -= crafted.count.toLong()
                        logger.info("extracted {}s, {} remaining", crafted, remaining)
                    } while (remaining > 0 && crafted.count > 0 && ItemStack.canCombine(crafted, resource.toStack()))
                    tx.commit()
                    (maxAmount - remaining)
                }

        override fun isResourceBlank() = forecast.isEmpty
        override fun getResource() = forecast.toVariant()
        override fun getAmount() = forecast.count.toLong()
        override fun getCapacity() = forecast.maxCount.toLong()

        override fun supportsInsertion() = false
        override fun insert(resource: ItemVariant, maxAmount: Long, txc: TransactionContext?) = 0L
    }
 */
}
