@file:Suppress("UnstableApiUsage")

package dev.adirelle.adicrafter.screen

import dev.adirelle.adicrafter.AdiCrafter.CRAFTER_SCREEN_HANDLER
import dev.adirelle.adicrafter.blockentity.CrafterBlockEntity
import dev.adirelle.adicrafter.screen.slotclick.SlotClickHandler
import dev.adirelle.adicrafter.screen.slotclick.SlotClickSpy
import dev.adirelle.adicrafter.utils.extension.toArray
import dev.adirelle.adicrafter.utils.extension.toItemString
import dev.adirelle.adicrafter.utils.lazyLogger
import io.github.cottonmc.cotton.gui.SyncedGuiDescription
import io.github.cottonmc.cotton.gui.ValidatedSlot
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
import net.minecraft.screen.slot.Slot
import net.minecraft.screen.slot.SlotActionType
import net.minecraft.screen.slot.SlotActionType.PICKUP
import net.minecraft.screen.slot.SlotActionType.QUICK_CRAFT

class CrafterScreenHandler(
    syncId: Int,
    playerInventory: PlayerInventory,
    private var blockEntity: CrafterBlockEntity? = null
) :
    SyncedGuiDescription(CRAFTER_SCREEN_HANDLER, syncId, playerInventory),
    CrafterBlockEntity.Listener {

    private val logger by lazyLogger()

    private val backingGrid = CraftingInventory(this, 3, 3)

    private val grid = SlotClickSpy(GridWrapper(backingGrid), "grid")
    private val result = SlotClickSpy(SimpleInventory(1), "result")
    private val buffer = SlotClickSpy(SimpleInventory(1), "buffer")
    private val output = SlotClickSpy(SimpleInventory(1), "output")

    init {
        with(rootPanel as WGridPanel) {
            val gridSlot = object : WItemSlot(grid, 0, 3, 3, false) {
                override fun createSlotPeer(inventory: Inventory, index: Int, x: Int, y: Int): ValidatedSlot =
                    GridSlot(inventory, index, x, y)
            }
            add(gridSlot, 0, 1)

            val resultSlot = WItemSlot.of(result, 0).apply { isModifiable = false }
            add(resultSlot, 4, 2)

            val bufferSlot = WItemSlot.of(buffer, 0).apply { isModifiable = false }
            add(bufferSlot, 8, 0)

            val outputSlot = WItemSlot.outputOf(output, 0).apply { isInsertingAllowed = false }
            add(outputSlot, 6, 2)

            add(createPlayerInventoryPanel(true), 0, 4, 9, 4)
        }
        rootPanel.validate(this)
    }

    override fun close(player: PlayerEntity) {
        super.close(player)
        blockEntity?.onListenerClosed(this)
    }

    var updating = false

    override fun onRecipeChanged(gridStacks: List<ItemStack>, resultStack: ItemStack) {
        try {
            logger.info("onRecipeChanged: {}, {}", toItemString(gridStacks), resultStack)
            updating = true
            gridStacks.forEachIndexed { idx, stack -> setStack(grid, idx, stack) }
            setStack(result, 0, resultStack)
        } finally {
            updating = false
        }
    }

    override fun onOutputChanged(bufferStack: ItemStack, outputStack: ItemStack) {
        logger.info("onOutputChanged: {}, {}", bufferStack, outputStack)
        setStack(buffer, 0, bufferStack)
        setStack(output, 0, outputStack)
    }

    private fun setStack(inventory: Inventory, slotIndex: Int, stack: ItemStack) {
        getSlotIndex(inventory, slotIndex).ifPresentOrElse(
            { slots[it].stack = stack.copy() },
            { logger.warn("slot not found, {} {}", inventory, slotIndex) }
        )
    }

    override fun onSlotClick(slotIndex: Int, button: Int, actionType: SlotActionType, player: PlayerEntity) {
        if (!interceptSlotClick(slotIndex, button, actionType, player)) {
            super.onSlotClick(slotIndex, button, actionType, player)
        }
    }

    private fun interceptSlotClick(
        slotIndex: Int,
        button: Int,
        actionType: SlotActionType,
        player: PlayerEntity
    ): Boolean {
        if (slotIndex !in 0 until slots.size) return false
        val slot = slots[slotIndex]
        val handler = slot.inventory as? SlotClickHandler ?: return false
        return handler.handleSlotClick(slot, button, actionType, player)
    }

    override fun onContentChanged(inventory: Inventory) {
        if (updating) return
        val blockEntity = blockEntity ?: return
        if (inventory == backingGrid) {
            blockEntity.setRecipe(findRecipe(), grid.toArray())
        }
        super.onContentChanged(inventory)
    }

    private fun findRecipe(): CraftingRecipe? =
        world.recipeManager.getFirstMatch(RecipeType.CRAFTING, backingGrid, world).orElse(null)

    private inner class GridWrapper(backing: CraftingInventory) :
        SlotClickHandler.Abstract<CraftingInventory>(backing) {

        override fun handleSlotClick(
            slot: Slot,
            button: Int,
            actionType: SlotActionType,
            player: PlayerEntity
        ): Boolean {
            if (blockEntity == null) return true
            return when (actionType) {
                PICKUP ->
                    if (!cursorStack.isEmpty && (slot.stack.isEmpty || !ItemStack.canCombine(
                            cursorStack,
                            slot.stack
                        ))
                    )
                        setSlot(slot, cursorStack)
                    else
                        clearSlot(slot)
                QUICK_CRAFT ->
                    setSlot(slot, cursorStack)
                else -> {
                    logger.info("ignoring action {} on #{}", actionType, slot.index)
                    true
                }
            }
        }

        private fun setSlot(slot: Slot, stack: ItemStack): Boolean {
            if (stack.isEmpty) return clearSlot(slot)
            logger.info("putting {} at #{}", stack, slot.index)
            slot.stack = stack.copy().apply { count = 1 }
            return true
        }

        private fun clearSlot(slot: Slot): Boolean {
            logger.info("removing {} at #{}", slot.stack, slot.index)
            slot.stack = ItemStack.EMPTY
            return true
        }

        override fun removeStack(slot: Int, amount: Int): ItemStack {
            backing.removeStack(slot, amount)
            return ItemStack.EMPTY
        }

        override fun removeStack(slot: Int): ItemStack {
            backing.removeStack(slot)
            return ItemStack.EMPTY
        }
    }

    private class GridSlot(inventory: Inventory, index: Int, x: Int, y: Int) :
        ValidatedSlot(inventory, index, x, y) {

        override fun canTakePartial(player: PlayerEntity?) = false
        override fun canTakeItems(player: PlayerEntity?) = false
    }

}

