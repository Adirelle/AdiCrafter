@file:Suppress("UnstableApiUsage")

package dev.adirelle.adicrafter.crafter

import dev.adirelle.adicrafter.crafter.CrafterBlockEntity.Companion.CONTENT_SLOT
import dev.adirelle.adicrafter.crafter.CrafterBlockEntity.Companion.GRID_HEIGHT
import dev.adirelle.adicrafter.crafter.CrafterBlockEntity.Companion.GRID_SIZE
import dev.adirelle.adicrafter.crafter.CrafterBlockEntity.Companion.GRID_WIDTH
import dev.adirelle.adicrafter.crafter.CrafterBlockEntity.Companion.INVENTORY_SIZE
import dev.adirelle.adicrafter.crafter.CrafterBlockEntity.Companion.OUTPUT_SLOT
import dev.adirelle.adicrafter.crafter.CrafterBlockEntity.Companion.RESULT_SLOT
import dev.adirelle.adicrafter.utils.lazyLogger
import dev.adirelle.adicrafter.utils.withOuterTransaction
import io.github.cottonmc.cotton.gui.SyncedGuiDescription
import io.github.cottonmc.cotton.gui.widget.WGridPanel
import io.github.cottonmc.cotton.gui.widget.WItemSlot
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant
import net.fabricmc.fabric.api.transfer.v1.item.PlayerInventoryStorage
import net.fabricmc.fabric.api.transfer.v1.storage.Storage
import net.fabricmc.fabric.api.transfer.v1.storage.StorageUtil
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.inventory.Inventory
import net.minecraft.inventory.SimpleInventory
import net.minecraft.item.ItemStack
import net.minecraft.screen.ScreenHandler
import net.minecraft.screen.ScreenHandlerListener
import net.minecraft.screen.slot.Slot
import net.minecraft.screen.slot.SlotActionType
import net.minecraft.screen.slot.SlotActionType.*

class CrafterScreenHandler(
    syncId: Int,
    playerInventory: PlayerInventory,
    blockInventory: Inventory = SimpleInventory(INVENTORY_SIZE),
    private val blockEntity: CrafterBlockEntity? = null
) : SyncedGuiDescription(CrafterFeature.SCREEN_HANDLER_TYPE, syncId, playerInventory, blockInventory, null),
    ScreenHandlerListener {

    private val logger by lazyLogger

    init {
        with(rootPanel as WGridPanel) {
            val gridSlot =
                WItemSlot.of(blockInventory, 0, GRID_WIDTH, GRID_HEIGHT)
            add(gridSlot, 0, 1)

            val resultSlot = WItemSlot.of(blockInventory, RESULT_SLOT)
            resultSlot.isModifiable = false
            add(resultSlot, 4, 2)

            val bufferSlot = WItemSlot.of(blockInventory, CONTENT_SLOT)
            bufferSlot.isModifiable = false
            add(bufferSlot, 8, 0)

            val outputSlot = WItemSlot.outputOf(blockInventory, OUTPUT_SLOT)
            outputSlot.isInsertingAllowed = false
            add(outputSlot, 6, 2)

            add(createPlayerInventoryPanel(true), 0, 4)
        }
        rootPanel.validate(this)

        addListener(this)
    }

    override fun close(player: PlayerEntity) {
        super.close(player)
        blockEntity?.onScreenHandlerClosed(this)
    }

    private inline fun ifBlockInventory(slotId: Int, block: (Slot) -> Unit) {
        slots.getOrNull(slotId)?.let { slot ->
            if (slot.inventory === blockInventory) {
                block(slot)
            }
        }
    }

    override fun onSlotUpdate(handler: ScreenHandler, slotId: Int, stack: ItemStack) {}

    override fun onPropertyUpdate(handler: ScreenHandler, property: Int, value: Int) {
        if (handler != this) return
        logger.debug("onPropertyUpdate: {} -> {}", property, value)
    }

    override fun onSlotClick(slotIndex: Int, button: Int, actionType: SlotActionType, player: PlayerEntity) {
        if (world.isClient) return
        ifBlockInventory(slotIndex) { slot ->
            when {
                slot.index < GRID_SIZE    -> onGridClick(slot, actionType)
                slot.index == OUTPUT_SLOT -> onOutputClick(slot, actionType)
                else                      -> logger.debug("ignored slot action: {}", slot.index, actionType)
            }
            return
        }
        super.onSlotClick(slotIndex, button, actionType, player)
    }

    private fun onGridClick(slot: Slot, actionType: SlotActionType) {
        when {
            actionType == PICKUP_ALL           -> blockInventory.clear()
            !slot.stack.isOf(cursorStack.item) -> slot.stack = cursorStack
            else                               -> slot.stack = ItemStack.EMPTY
        }
    }

    private fun onOutputClick(slot: Slot, actionType: SlotActionType) {
        blockEntity?.storage?.let { storage ->
            withOuterTransaction { tx ->
                val moved = when (actionType) {
                    PICKUP     -> pickupOutput(storage, slot.stack.count.toLong(), tx)
                    PICKUP_ALL -> pickupOutput(storage, slot.stack.maxCount.toLong(), tx)
                    QUICK_MOVE -> quickMoveOutput(storage, slot.stack.maxCount.toLong(), tx)
                    else       -> {
                        logger.debug("ignored output action: {}", actionType)
                        0
                    }
                }
                if (moved > 0) tx.commit()
            }
        }
    }

    private fun pickupOutput(storage: Storage<ItemVariant>, maxAmount: Long, tx: Transaction) =
        StorageUtil.move(storage, PlayerInventoryStorage.getCursorStorage(this), { true }, maxAmount, tx)

    private fun quickMoveOutput(storage: Storage<ItemVariant>, maxAmount: Long, tx: Transaction) =
        playerInventory?.let { inv ->
            StorageUtil.move(storage, PlayerInventoryStorage.of(inv), { true }, maxAmount, tx)
        } ?: 0L

}
