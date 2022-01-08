@file:Suppress("UnstableApiUsage")

package dev.adirelle.adicrafter.crafter

import dev.adirelle.adicrafter.utils.Broadcaster
import dev.adirelle.adicrafter.utils.areEqual
import dev.adirelle.adicrafter.utils.expectSameSizeAs
import dev.adirelle.adicrafter.utils.extension.getItemStacks
import dev.adirelle.adicrafter.utils.extension.putItemStacks
import dev.adirelle.adicrafter.utils.extension.toItemString
import dev.adirelle.adicrafter.utils.extension.withNestedTransaction
import dev.adirelle.adicrafter.utils.general.lazyLogger
import dev.adirelle.adicrafter.utils.ifDifferent
import dev.adirelle.adicrafter.utils.minecraft.CraftingRecipeResolver
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext
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

    private val logger by lazyLogger

    private var dirtyRecipe = false
    private var dirtyForecast = false

    private var grid: MutableList<ItemStack> = MutableList(GRID_SIZE) { ItemStack.EMPTY }
    private var forecast: ItemStack = ItemStack.EMPTY

    private var bulkCrafter: BulkCraftingStorage = object : BulkCraftingStorage() {
        override fun markDirty() {
            dirtyForecast = true
        }
    }
    private var recipe by bulkCrafter::recipe

    private var buffer: TransactionalSingleStackStorage = object : TransactionalSingleStackStorage() {
        override fun markDirty() {
            super.markDirty()
            dirtyForecast = true
            this@CrafterBlockEntity.markDirty()
        }
    }

    val crafter = CraftingStorage(buffer, bulkCrafter)

    data class DisplayState(
        val grid: List<ItemStack>,
        val recipe: Optional<CraftingRecipe>,
        val buffer: ItemStack,
        val forecast: ItemStack
    )

    val displayBroadcaster = Broadcaster<DisplayState>()
    fun getDisplayState() = DisplayState(grid, recipe, buffer.toStack(), forecast)

    fun tick(world: World) {
        var shouldBroadcast = dirtyRecipe
        updateRecipe(world)
        shouldBroadcast = shouldBroadcast || dirtyForecast
        updateForecast()
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
        if (!dirtyRecipe) return
        recipe = CraftingRecipeResolver.of(world).resolve(grid)
        dirtyRecipe = false
    }

    private fun updateForecast(txc: TransactionContext? = null) {
        if (!dirtyForecast || !displayBroadcaster.hasListeners()) return
        forecast = ItemStack.EMPTY
        val maxAmount = recipe.map { it.output.count.toLong() }.orElse(buffer.amount)
        withNestedTransaction(txc) { tx ->
            for (view in crafter.iterable(tx)) {
                val resource = view.resource
                if (resource.isBlank) continue
                val amount = view.extract(resource, maxAmount, tx)
                if (amount == 0L) continue
                forecast = resource.toStack(amount.toInt())
            }
        }
        dirtyForecast = false
    }

    override fun getDisplayName(): Text = TranslatableText("block.adicrafter.crafter")

    override fun createMenu(syncId: Int, playerInventory: PlayerInventory, player: PlayerEntity) =
        CrafterScreenHandler(syncId, playerInventory, this)

    override fun readNbt(nbt: NbtCompound) {
        super.readNbt(nbt)

        buffer.readNbt(nbt.getCompound(BUFFER_NBT_KEY))

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
        nbt.put(BUFFER_NBT_KEY, NbtCompound().also { buffer.writeNbt(it) })

        logger.info("written to NBT: {}, {}", buffer, toItemString(grid))
    }
}
