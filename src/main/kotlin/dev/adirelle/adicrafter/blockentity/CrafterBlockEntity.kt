package dev.adirelle.adicrafter.blockentity

import dev.adirelle.adicrafter.AdiCrafter.CRAFTER_BLOCK_ENTITY
import dev.adirelle.adicrafter.screen.CrafterScreenHandler
import dev.adirelle.adicrafter.utils.extension.toItemString
import dev.adirelle.adicrafter.utils.lazyLogger
import net.fabricmc.fabric.api.util.NbtType
import net.minecraft.block.BlockState
import net.minecraft.block.entity.BlockEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NbtCompound
import net.minecraft.nbt.NbtList
import net.minecraft.recipe.CraftingRecipe
import net.minecraft.screen.NamedScreenHandlerFactory
import net.minecraft.screen.ScreenHandler
import net.minecraft.text.Text
import net.minecraft.text.TranslatableText
import net.minecraft.util.Identifier
import net.minecraft.util.math.BlockPos

@Suppress("UnstableApiUsage")
class CrafterBlockEntity(pos: BlockPos, state: BlockState) :
    BlockEntity(CRAFTER_BLOCK_ENTITY, pos, state), NamedScreenHandlerFactory {

    companion object {

        private const val RECIPE_NBT_KEY = "Recipe"
        private const val INVENTORY_NBT_KEY = "Inventory"

        private const val GRID_SIZE = 9
        private const val RESULT_SLOT = GRID_SIZE
        private const val BUFFER_SLOT = RESULT_SLOT + 1
        private const val OUTPUT_SLOT = BUFFER_SLOT + 1
        private const val INVENTORY_SIZE = OUTPUT_SLOT + 1
    }

    private val logger by lazyLogger()

    private val stacks = MutableList<ItemStack>(INVENTORY_SIZE) { ItemStack.EMPTY }
    private var recipe: Identifier? = null

    private var currentScreen: CrafterScreenHandler? = null

    override fun createMenu(syncId: Int, playerInventory: PlayerInventory, player: PlayerEntity): ScreenHandler {
        currentScreen = CrafterScreenHandler(syncId, playerInventory, this)
        updateScreenRecipe()
        updateScreenOutput()
        return currentScreen!!
    }

    fun onMenuClosed(screenHandler: CrafterScreenHandler) {
        if (screenHandler == currentScreen) {
            currentScreen = null
        }
    }

    private fun updateScreenRecipe() {
        logger.info("updateScreenRecipe: {}", toItemString(stacks.subList(0, RESULT_SLOT)))
        currentScreen?.updateRecipeFromBlockEntity(stacks.subList(0, GRID_SIZE), stacks[RESULT_SLOT])
    }

    private fun updateScreenOutput() {
        logger.info("updateScreenOutput: {}", toItemString(stacks.subList(BUFFER_SLOT, OUTPUT_SLOT)))
        currentScreen?.updateOutputFromBlockEntity(stacks[BUFFER_SLOT], stacks[OUTPUT_SLOT])
    }

    fun updateRecipeFromScreen(recipe: CraftingRecipe?, grid: Array<ItemStack>) {
        logger.info("updateRecipeFromScreen: {} {}", recipe, toItemString(grid))

        this.recipe = recipe?.id
        for (i in 0 until GRID_SIZE) {
            stacks[i] = grid[i].copy()
        }
        stacks[RESULT_SLOT] = recipe?.output?.copy() ?: ItemStack.EMPTY
        updateScreenRecipe()
        updateOutput()
        markDirty()
    }

    private fun updateOutput() {
        val buffer = stacks[BUFFER_SLOT]
        val result = stacks[RESULT_SLOT]
        stacks[OUTPUT_SLOT] = (if (buffer.isEmpty) result else buffer).copy()
        updateScreenOutput()
    }

    override fun getDisplayName(): Text = TranslatableText("block.adicrafter.crafter")

    override fun readNbt(nbt: NbtCompound) {
        super.readNbt(nbt)
        recipe = Identifier.tryParse(nbt.getString(RECIPE_NBT_KEY))
        val itemList = nbt.getList(INVENTORY_NBT_KEY, NbtType.COMPOUND)
        for (i in 0 until INVENTORY_SIZE) {
            stacks[i] = (itemList[i] as? NbtCompound)?.let {
                ItemStack.fromNbt(it)
            } ?: ItemStack.EMPTY
        }
    }

    override fun writeNbt(nbt: NbtCompound) {
        super.writeNbt(nbt)
        nbt.putString(RECIPE_NBT_KEY, recipe.toString())
        nbt.put(INVENTORY_NBT_KEY, NbtList().apply {
            for (i in 0 until INVENTORY_SIZE) {
                add(NbtCompound().also {
                    stacks[i].writeNbt(it)
                })
            }
        })
    }

}
