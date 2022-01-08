@file:Suppress("UnstableApiUsage")

package dev.adirelle.adicrafter.crafter

import dev.adirelle.adicrafter.crafter.CrafterBlockEntity.DisplayState
import dev.adirelle.adicrafter.utils.expectExactSize
import dev.adirelle.adicrafter.utils.extension.asList
import dev.adirelle.adicrafter.utils.extension.close
import dev.adirelle.adicrafter.utils.extension.set
import dev.adirelle.adicrafter.utils.extension.toItemString
import dev.adirelle.adicrafter.utils.ifDifferent
import dev.adirelle.adicrafter.utils.lazyLogger
import io.github.cottonmc.cotton.gui.SyncedGuiDescription
import io.github.cottonmc.cotton.gui.widget.WGridPanel
import io.github.cottonmc.cotton.gui.widget.WItemSlot
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.inventory.SimpleInventory
import net.minecraft.item.ItemStack
import net.minecraft.screen.slot.Slot
import net.minecraft.screen.slot.SlotActionType
import net.minecraft.screen.slot.SlotActionType.*

class CrafterScreenHandler(
    syncId: Int, playerInventory: PlayerInventory,
    blockEntity: CrafterBlockEntity? = null
) : SyncedGuiDescription(Crafter.SCREEN_HANDLER_TYPE, syncId, playerInventory) {

    private val logger by lazyLogger()

    private val grid = SimpleInventory(CrafterBlockEntity.GRID_SIZE)
    private val result = SimpleInventory(1)
    private val buffer = SimpleInventory(1)
    private val forecast = SimpleInventory(1)

    init {
        with(rootPanel as WGridPanel) {
            val gridSlot = WItemSlot.of(grid, 0, CrafterBlockEntity.GRID_WIDTH, CrafterBlockEntity.GRID_HEIGHT)
                .apply { isModifiable = false }
            add(gridSlot, 0, 1)

            val resultSlot = WItemSlot.of(result, 0).apply { isModifiable = false }
            add(resultSlot, 4, 2)

            val bufferSlot = WItemSlot.of(buffer, 0).apply { isModifiable = false }
            add(bufferSlot, 8, 0)

            val outputSlot = WItemSlot.outputOf(forecast, 0).apply { isModifiable = false }
            add(outputSlot, 6, 2)

            add(createPlayerInventoryPanel(true), 0, 4)
        }
        rootPanel.validate(this)
    }

    private val subscriptions = blockEntity?.let { onServer(it) } ?: listOf()

    private fun onServer(blockEntity: CrafterBlockEntity): List<AutoCloseable> =
        listOf(
            blockEntity.displayBroadcaster.addListener(this::updateDisplay)
        ).also {
            updateDisplay(blockEntity.getDisplayState())
            grid.addListener { onGridEditedLocally(blockEntity) }
        }

    override fun close(player: PlayerEntity) {
        super.close(player)
        subscriptions.close()
    }

    private var updatingGrid = false

    private fun onGridEditedLocally(blockEntity: CrafterBlockEntity) {
        if (updatingGrid) return
        logger.info("sending grid to the blockEntity: {}", toItemString(grid))
        blockEntity.setGrid(grid.asList())
    }

    private fun updateDisplay(displayState: DisplayState) {
        val (newGrid, newRecipe, newBuffer, newForecast) = displayState
        buffer[0] = newBuffer.copy()
        forecast[0] = newForecast.copy()
        result[0] = newRecipe.map { it.output }.orElse(ItemStack.EMPTY)

        ifDifferent(grid, newGrid) {
            try {
                updatingGrid = true
                expectExactSize(newGrid, grid.size()).forEachIndexed { index, stack ->
                    getSlotIndex(grid, index).ifPresent { slotIndex ->
                        slots[slotIndex].stack = stack.copy()
                    }
                }
            } finally {
                updatingGrid = false
            }
        }
    }

    override fun onSlotClick(slotIndex: Int, button: Int, actionType: SlotActionType, player: PlayerEntity) {
        if (world.isClient) return
        slots.getOrNull(slotIndex)?.let { slot ->
            return when (slot.inventory) {
                grid     -> onGridClick(slot, actionType)
                forecast -> onOutputClick(slot, actionType)
                else     -> super.onSlotClick(slotIndex, button, actionType, player)
            }
        }
        super.onSlotClick(slotIndex, button, actionType, player)
    }

    private fun onGridClick(slot: Slot, actionType: SlotActionType) {
        if (actionType == PICKUP) {
            if (!cursorStack.isEmpty && (slot.stack.isEmpty || !ItemStack.canCombine(cursorStack, slot.stack))) {
                slot.stack = cursorStack.copy().apply { count = 1 }
            } else {
                slot.stack = ItemStack.EMPTY
            }
            return
        }

        if (actionType == QUICK_CRAFT) {
            slot.stack = cursorStack.copy().apply { count = 1 }
            return
        }

        if (actionType == QUICK_MOVE) {
            slot.stack = ItemStack.EMPTY
            return
        }

        logger.info("ignored onGridClick: #{} {}", slot.index, actionType)
    }

    private fun onOutputClick(slot: Slot, actionType: SlotActionType) {
        return
        /*
        val crafter = crafter ?: return
        if (actionType != PICKUP && actionType != QUICK_MOVE) {
            logger.info("ignored onOutputClick: #{} {}", slot.index, actionType)
            return
        }
        inOuterTransaction { tx ->
            if (actionType == PICKUP) {
                val cursor = PlayerInventoryStorage.getCursorStorage(this)
                doCraft(cursor, min(cursor.capacity - cursor.amount, crafter.amount), cursor.resource, tx)
            } else {
                doCraft(PlayerInventoryStorage.of(playerInventory), crafter.capacity, crafter.resource, tx)
            }
            tx.commit()
        }

         */
    }

    /*
    private fun doCraft(target: Storage<ItemVariant>, maxAmount: Long, resource: ItemVariant, tx: Transaction) {
        logger.info("trying to move {} {} from {} to {}", maxAmount, toItemString(resource), crafter, target)

        val filter: Predicate<ItemVariant> =
            if (resource.isBlank) Predicate { true }
            else Predicate.isEqual(resource)

        val extracted = StorageUtil.move(crafter, target, filter, maxAmount, tx)
        logger.info("crafted {}", extracted)
    }
     */

}
