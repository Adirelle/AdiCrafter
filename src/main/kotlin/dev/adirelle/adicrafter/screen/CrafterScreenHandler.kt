@file:Suppress("UnstableApiUsage")

package dev.adirelle.adicrafter.screen

import dev.adirelle.adicrafter.AdiCrafter.CRAFTER_SCREEN_HANDLER
import dev.adirelle.adicrafter.blockentity.CrafterBlockEntity
import dev.adirelle.adicrafter.blockentity.CrafterBlockEntity.Config
import dev.adirelle.adicrafter.screen.slotclick.SlotClickHandler
import dev.adirelle.adicrafter.screen.slotclick.SlotClickSpy
import dev.adirelle.adicrafter.utils.extension.copyFrom
import dev.adirelle.adicrafter.utils.extension.toArray
import dev.adirelle.adicrafter.utils.lazyLogger
import dev.adirelle.adicrafter.utils.onChangeCallback
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

class CrafterScreenHandler(syncId: Int, playerInventory: PlayerInventory) :
    SyncedGuiDescription(CRAFTER_SCREEN_HANDLER, syncId, playerInventory) {

    private val logger by lazyLogger()

    private val grid = CraftingInventory(this, 3, 3)
    private val result = SimpleInventory(1)

    private val buffer = SimpleInventory(1)
    private val output = SimpleInventory(1)

    private var config by onChangeCallback(Config.EMPTY, this::onConfigChanged)

    private var batchUpdate = false

    init {
        with(rootPanel as WGridPanel) {
            val wrappedGrid = SlotClickSpy(GridWrapper(grid), "grid")
            add(WGridItemSlot(wrappedGrid), 0, 1)

            val resultSlot = WItemSlot.of(SlotClickSpy(result, "result"), 0).apply { isModifiable = false }
            add(resultSlot, 4, 2)

            val bufferSlot = WItemSlot.of(SlotClickSpy(buffer, "buffer"), 0).apply { isModifiable = false }
            add(bufferSlot, 8, 0)

            val outputSlot = WItemSlot.outputOf(SlotClickSpy(output, "output"), 0).apply { isInsertingAllowed = false }
            add(outputSlot, 6, 2)

            add(createPlayerInventoryPanel(true), 0, 4, 9, 4)
        }
        rootPanel.validate(this)
    }

    private var blockEntity: CrafterBlockEntity? = null

    constructor(
        syncId: Int,
        playerInventory: PlayerInventory,
        be: CrafterBlockEntity
    ) : this(syncId, playerInventory) {
        blockEntity = be

        config = be.config
        buffer.setStack(0, be.buffer.copy())
        output.setStack(0, be.output.copy())

        buffer.addListener { be.buffer = it.getStack(0).copy() }
        output.addListener { be.output = it.getStack(0).copy() }
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

    /** Called on crafting grid changes. Use it to update the recipe. */
    override fun onContentChanged(inventory: Inventory?) {
        super.onContentChanged(inventory)
        if (!batchUpdate && inventory == grid) {
            val recipe = findRecipe()
            config =
                Config(
                    recipe?.id ?: Config.EMPTY_ID,
                    grid.toArray(),
                    recipe?.output?.copy() ?: ItemStack.EMPTY
                )
        }
    }

    /** Try to find a crafting recipe matching the grid content */
    private fun findRecipe(): CraftingRecipe? =
        world.recipeManager.getFirstMatch(RecipeType.CRAFTING, grid, world).orElse(null)

    override fun dropInventory(player: PlayerEntity?, inventory: Inventory?) {
        logger.info("dropInventory({}, {})", player, inventory)
        super.dropInventory(player, inventory)
    }

    override fun insertItem(stack: ItemStack?, startIndex: Int, endIndex: Int, fromLast: Boolean): Boolean {
        logger.info("insertItem({}, {}, {}, {}))", stack, startIndex, endIndex, fromLast)
        return super.insertItem(stack, startIndex, endIndex, fromLast)
    }

    private inner class GridWrapper(backing: CraftingInventory) :
        SlotClickHandler.Abstract<CraftingInventory>(backing) {

        override fun handleSlotClick(
            slot: Slot,
            button: Int,
            actionType: SlotActionType,
            player: PlayerEntity
        ): Boolean {
            if (batchUpdate || blockEntity == null) return true
            return when (actionType) {
                PICKUP      ->
                    if (!cursorStack.isEmpty && (slot.stack.isEmpty || !ItemStack.canCombine(cursorStack, slot.stack)))
                        setSlot(slot, cursorStack)
                    else
                        clearSlot(slot)
                QUICK_CRAFT ->
                    setSlot(slot, cursorStack)
                else        -> {
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

    private class WGridItemSlot(grid: Inventory) : WItemSlot(grid, 0, 3, 3, false) {

        override fun createSlotPeer(inventory: Inventory, index: Int, x: Int, y: Int): ValidatedSlot =
            GridSlot(inventory, index, x, y)
    }

    private class GridSlot(inventory: Inventory, index: Int, x: Int, y: Int) :
        ValidatedSlot(inventory, index, x, y) {

        override fun canTakePartial(player: PlayerEntity?) = false
        override fun canTakeItems(player: PlayerEntity?) = false
    }
}

