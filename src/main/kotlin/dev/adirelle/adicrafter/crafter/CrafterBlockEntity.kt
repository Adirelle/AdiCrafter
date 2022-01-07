package dev.adirelle.adicrafter.crafter

import dev.adirelle.adicrafter.utils.DefaultNotifier
import dev.adirelle.adicrafter.utils.extension.*
import dev.adirelle.adicrafter.utils.lazyLogger
import net.minecraft.block.BlockState
import net.minecraft.block.entity.BlockEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.inventory.CraftingInventory
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NbtCompound
import net.minecraft.recipe.CraftingRecipe
import net.minecraft.recipe.RecipeType
import net.minecraft.screen.NamedScreenHandlerFactory
import net.minecraft.screen.ScreenHandler
import net.minecraft.server.world.ServerWorld
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

    private var loading = true

    val gridNotifier = DefaultNotifier<Array<ItemStack>>()
    var grid = Array(9) { ItemStack.EMPTY }
        set(value) {
            if (!value.contentEquals(field)) {
                field = value
                dirtyRecipe = true
                if (!loading) {
                    markDirty()
                }
                gridNotifier.notify(value)
            }
        }

    val bufferNotifier = DefaultNotifier<ItemStack>()
    var buffer: ItemStack = ItemStack.EMPTY
        set(value) {
            if (!ItemStack.areEqual(field, value)) {
                field = value
                if (!loading) {
                    markDirty()
                }
                bufferNotifier.notify(value)
            }
        }

    val forecastNotifier = DefaultNotifier<ItemStack>()
    var forecast: ItemStack = ItemStack.EMPTY
        set(value) {
            if (!ItemStack.areEqual(field, value)) {
                field = value
                forecastNotifier.notify(value)
            }
        }

    private var dirtyRecipe = false
    val recipeNotifier = DefaultNotifier<Optional<CraftingRecipe>>()
    var recipe = Optional.empty<CraftingRecipe>()
        set(value) {
            if (field != value) {
                field = value
                recipeNotifier.notify(value)
            }
        }
        get() {
            if (dirtyRecipe) {
                dirtyRecipe = false
                recipe = resolveRecipe(grid)
            }
            return field
        }

    override fun getDisplayName(): Text = TranslatableText("block.adicrafter.crafter")

    override fun createMenu(syncId: Int, playerInventory: PlayerInventory, player: PlayerEntity) =
        CrafterScreenHandler(syncId, playerInventory, this)

    override fun setWorld(world: World?) {
        super.setWorld(world)
        if (world != null) {
            loading = false
        }
    }

    override fun readNbt(nbt: NbtCompound) {
        super.readNbt(nbt)

        buffer = nbt.getItemStack(BUFFER_NBT_KEY)
        grid = nbt.getItemStacks(GRID_NBT_KEY).toTypedArray()

        logger.info("read from NBT: {}, {}", buffer, toItemString(grid))
    }

    override fun writeNbt(nbt: NbtCompound) {
        super.writeNbt(nbt)

        nbt.putItemStacks(GRID_NBT_KEY, grid.toList())
        nbt.putItemStack(BUFFER_NBT_KEY, buffer)

        logger.info("written to NBT: {}, {}", buffer, toItemString(grid))
    }

    private fun resolveRecipe(grid: Array<ItemStack>): Optional<CraftingRecipe> {
        val craftingGrid by lazy {
            CraftingInventory(
                object : ScreenHandler(null, 0) {
                    override fun canUse(player: PlayerEntity?) = false
                },
                GRID_WIDTH, GRID_HEIGHT
            )
        }

        return Optional
            .ofNullable(world as? ServerWorld)
            .filter { !grid.all { it.isEmpty } }
            .flatMap { world ->
                for (idx in 0 until GRID_SIZE) {
                    craftingGrid.setStack(idx, grid[idx])
                }
                world.recipeManager.getFirstMatch(RecipeType.CRAFTING, craftingGrid, world)
            }
            .tap { logger.info("found recipe: {} -> {}", toItemString(grid), it) }
            .instanceOf()
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
