package dev.adirelle.adicrafter.crafter

import dev.adirelle.adicrafter.utils.extension.*
import dev.adirelle.adicrafter.utils.lazyLogger
import net.fabricmc.fabric.api.util.NbtType
import net.minecraft.block.BlockState
import net.minecraft.block.entity.BlockEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NbtCompound
import net.minecraft.recipe.CraftingRecipe
import net.minecraft.screen.NamedScreenHandlerFactory
import net.minecraft.screen.ScreenHandler
import net.minecraft.text.Text
import net.minecraft.text.TranslatableText
import net.minecraft.util.Identifier
import net.minecraft.util.math.BlockPos
import readNbt
import java.util.*

@Suppress("UnstableApiUsage")
class CrafterBlockEntity(pos: BlockPos, state: BlockState) :
    BlockEntity(Crafter.BLOCK_ENTITY_TYPE, pos, state),
    NamedScreenHandlerFactory {

    companion object {

        private const val RECIPE_NBT_KEY = "Recipe"
        private const val GRID_NBT_KEY = "Grid"
        private const val RESULT_NBT_KEY = "Result"
        private const val BUFFER_NBT_KEY = "Buffer"

        private const val GRID_SIZE = 9
    }

    private val logger by lazyLogger()

    private val grid = MutableList<ItemStack>(GRID_SIZE) { ItemStack.EMPTY }
    private var recipeId: Identifier? = null
    private var recipeOutput = ItemStack.EMPTY
    private var buffer = ItemStack.EMPTY

    private var listeners = ListenerList()

    override fun createMenu(syncId: Int, playerInventory: PlayerInventory, player: PlayerEntity): ScreenHandler {
        logger.info("opening GUI")
        val newScreenHandler = CrafterScreenHandler(syncId, playerInventory, this)
        listeners.add(newScreenHandler)
        updateListenerRecipe(newScreenHandler)
        updateListenerOutput(newScreenHandler)
        return newScreenHandler
    }

    fun onListenerClosed(listener: Listener) {
        logger.info("GUI closed")
        listeners.remove(listener)
    }

    private fun updateListenerRecipe(listener: Listener = listeners) {
        logger.info("updateListenerRecipe")
        listener.onRecipeChanged(grid, recipeOutput)
    }

    private fun updateListenerOutput(listener: Listener = listeners) {
        logger.info("updateListenerOutput")
    }

    fun setRecipe(newRecipe: CraftingRecipe?, newGrid: Array<ItemStack>) {
        logger.info("setRecipe: {} {}", recipeId, toItemString(grid))

        grid.indices.forEach { grid[it] = newGrid[it].copy() }
        markDirty()

        val newRecipeId = newRecipe?.id
        val newRecipeOutput = newRecipe?.output ?: ItemStack.EMPTY
        if (newRecipeId == recipeId && ItemStack.areEqual(recipeOutput, newRecipeOutput)) return
        recipeId = newRecipeId
        recipeOutput = newRecipeOutput

        isSimDirty = true
        updateListenerRecipe()
        updateOutputSim()
    }

    private var isSimDirty = true

    private fun updateOutputSim() {
        if (listeners.isEmpty() || !isSimDirty) return
        logger.info("updateOutputSim")
        isSimDirty = false

//
//        transactional(txc) { tx ->
//            val extractable = storage.extract(ItemVariant.blank(), 1, tx)
//            if (extractable > 0) {
//            } else {
//
//            }
//        }
    }

    override fun getDisplayName(): Text = TranslatableText("block.adicrafter.crafter")

    override fun readNbt(nbt: NbtCompound) {
        super.readNbt(nbt)

        recipeId = nbt.getIdentifier(RECIPE_NBT_KEY)
        grid.readNbt(nbt.getList(GRID_NBT_KEY, NbtType.COMPOUND))
        recipeOutput = nbt.getItemStack(RESULT_NBT_KEY)
        buffer = nbt.getItemStack(BUFFER_NBT_KEY)

        logger.info("read from NBT: {}, {}, {}, {}", recipeId, toItemString(grid), recipeOutput, buffer)

        updateListenerRecipe()
        updateOutputSim()
    }

    override fun writeNbt(nbt: NbtCompound) {
        super.writeNbt(nbt)
        logger.info("writing to NBT: {}, {}, {}, {}", recipeId, toItemString(grid), recipeOutput, buffer)

        nbt.putIdentifier(RECIPE_NBT_KEY, recipeId)
        nbt.putItemStacks(GRID_NBT_KEY, grid)
        nbt.putItemStack(RESULT_NBT_KEY, recipeOutput)
        nbt.putItemStack(BUFFER_NBT_KEY, buffer)
    }

    interface Listener {

        fun onRecipeChanged(gridStacks: List<ItemStack>, resultStack: ItemStack)
        fun onOutputChanged(bufferStack: ItemStack, outputStack: ItemStack)
    }

    private class ListenerList : Listener {

        private val listeners: MutableList<Listener> = Collections.synchronizedList(ArrayList(2))

        fun isEmpty() = listeners.isEmpty()

        override fun onRecipeChanged(gridStacks: List<ItemStack>, resultStack: ItemStack) {
            logger.info("updating listener recipes: {}, {}", toItemString(gridStacks), resultStack)
            synchronized(listeners) {
                listeners.forEach { it.onRecipeChanged(gridStacks, resultStack) }
            }
        }

        override fun onOutputChanged(bufferStack: ItemStack, outputStack: ItemStack) {
            logger.info("updating listener output: {}, {}", toItemString(bufferStack), outputStack)
            synchronized(listeners) {
                listeners.forEach { it.onOutputChanged(bufferStack, outputStack) }
            }
        }

        fun add(listener: Listener) {
            logger.info("adding listener: {}", listener)
            listeners.add(listener)
        }

        fun remove(listener: Listener) {
            logger.info("removing listener: {}", listener)
            listeners.remove(listener)
        }
    }

}
