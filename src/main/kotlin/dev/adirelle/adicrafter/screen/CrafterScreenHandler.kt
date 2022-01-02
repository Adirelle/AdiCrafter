@file:Suppress("UnstableApiUsage")

package dev.adirelle.adicrafter.screen

import dev.adirelle.adicrafter.AdiCrafter.CRAFTER_SCREEN_HANDLER
import dev.adirelle.adicrafter.blockentity.CrafterBlockEntity
import dev.adirelle.adicrafter.blockentity.CrafterBlockEntity.Config
import dev.adirelle.adicrafter.utils.extension.copyFrom
import dev.adirelle.adicrafter.utils.extension.toArray
import dev.adirelle.adicrafter.utils.lazyLogger
import dev.adirelle.adicrafter.utils.onChangeCallback
import io.github.cottonmc.cotton.gui.SyncedGuiDescription
import io.github.cottonmc.cotton.gui.widget.WGridPanel
import io.github.cottonmc.cotton.gui.widget.WItemSlot
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.inventory.CraftingInventory
import net.minecraft.inventory.Inventory
import net.minecraft.inventory.SimpleInventory
import net.minecraft.item.ItemStack
import net.minecraft.recipe.CraftingRecipe
import net.minecraft.recipe.RecipeType
import net.minecraft.screen.slot.SlotActionType

class CrafterScreenHandler(
    syncId: Int,
    playerInventory: PlayerInventory,
    private val blockEntity: CrafterBlockEntity? = null
) : SyncedGuiDescription(CRAFTER_SCREEN_HANDLER, syncId, playerInventory) {

    private val logger by lazyLogger()

    private val grid = CraftingInventory(this, 3, 3)
    private val result = SimpleInventory(1)

    private val buffer = SimpleInventory(1)
    private val output = SimpleInventory(1)

    private var config by onChangeCallback(Config.EMPTY, this::onConfigChanged)

    private var batchUpdate = false

    init {
        with(rootPanel as WGridPanel) {
            add(WItemSlot.of(grid, 0, 3, 3), 0, 1)

            val resultSlot = WItemSlot.of(result, 0).apply { isModifiable = false }
            add(resultSlot, 4, 2)

            val bufferSlot = WItemSlot.of(buffer, 0).apply { isModifiable = false }
            add(bufferSlot, 8, 0)

            val outputSlot = WItemSlot.outputOf(output, 0).apply { isInsertingAllowed = false }
            add(outputSlot, 6, 2)

            add(createPlayerInventoryPanel(true), 0, 4, 9, 4)
        }
        rootPanel.validate(this)

        blockEntity?.let { be ->
            config = be.config
            buffer.setStack(0, be.buffer.copy())
            buffer.addListener { be.buffer = it.getStack(0).copy() }
            output.setStack(0, be.output.copy())
            output.addListener { be.output = it.getStack(0).copy() }
        }
    }

    private fun onConfigChanged() {
        logger.info("new config: {}", config)
        try {
            batchUpdate = true
            grid.copyFrom(config.grid)
            result.setStack(0, config.result.copy())
        } finally {
            batchUpdate = false
        }
        grid.markDirty()
        result.markDirty()
        blockEntity?.let { it.config = config }
    }

    /**
     * Override the default behavior of the grafting grid
     */
    override fun onSlotClick(slotIndex: Int, button: Int, actionType: SlotActionType?, player: PlayerEntity?) {
        if (!interceptSlotClick(slotIndex)) {
            super.onSlotClick(slotIndex, button, actionType, player)
        }
    }

    private fun interceptSlotClick(slotIndex: Int): Boolean {
        if (slotIndex < 0 || slotIndex >= slots.size) return false
        val slot = slots[slotIndex]
        if (slot.inventory != grid) return false
        if (batchUpdate || blockEntity == null) return true
        if (!cursorStack.isEmpty && (slot.stack.isEmpty || !ItemStack.canCombine(cursorStack, slot.stack))) {
            logger.info("#{}: replacing {} by a copy of {}", slot.index, slot.stack, cursorStack)
            slot.stack = cursorStack.copy().apply { count = 1 }
        } else {
            logger.info("#{}: removing {}", slot.index, slot.stack)
            slot.stack = ItemStack.EMPTY
        }
        return true
    }

    /**
     * Called on crafting grid changes. Use it to update the recipe.
     */

    override fun onContentChanged(inventory: Inventory?) {
        super.onContentChanged(inventory)
        if (!batchUpdate && inventory == grid && blockEntity != null) {
            val recipe = findRecipe()
            config = Config(
                recipe?.id ?: Config.EMPTY_ID,
                grid.toArray(),
                recipe?.output?.copy() ?: ItemStack.EMPTY
            )
        }
    }

    /**
     * Try to find a crafting recipe matching the grid content
     */
    private fun findRecipe(): CraftingRecipe? =
        world.recipeManager
            .getFirstMatch(RecipeType.CRAFTING, grid, world)
            .orElse(null)
}
