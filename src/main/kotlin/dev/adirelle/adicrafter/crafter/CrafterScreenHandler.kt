@file:Suppress("UnstableApiUsage")

package dev.adirelle.adicrafter.crafter

import dev.adirelle.adicrafter.crafter.CrafterBlockEntity.*
import dev.adirelle.adicrafter.utils.LockableEmitter
import dev.adirelle.adicrafter.utils.extension.close
import dev.adirelle.adicrafter.utils.extension.inOuterTransaction
import dev.adirelle.adicrafter.utils.extension.toArray
import dev.adirelle.adicrafter.utils.extension.toItemString
import dev.adirelle.adicrafter.utils.lazyLogger
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
import net.minecraft.inventory.SimpleInventory
import net.minecraft.item.ItemStack
import net.minecraft.screen.slot.Slot
import net.minecraft.screen.slot.SlotActionType
import net.minecraft.screen.slot.SlotActionType.*
import java.util.function.Predicate
import kotlin.math.min

class CrafterScreenHandler(
    syncId: Int, playerInventory: PlayerInventory, blockEntity: CrafterBlockEntity? = null
) : SyncedGuiDescription(Crafter.SCREEN_HANDLER_TYPE, syncId, playerInventory) {

    private val logger by lazyLogger()

    private var gridUpdateEmitter: LockableEmitter<GridUpdate>? = null
    private val crafter = blockEntity?.storage

    private val grid =
        if (blockEntity !== null) {
            val emitter = LockableEmitter(blockEntity.gridUpdateEmitter)
            gridUpdateEmitter = emitter
            object : SimpleInventory(CrafterBlockEntity.GRID_SIZE) {
                override fun markDirty() {
                    super.markDirty()
                    emitter.emit(GridUpdate(this.toArray()))
                }
            }
        } else
            SimpleInventory(CrafterBlockEntity.GRID_SIZE)

    private val result = SimpleInventory(1)
    private val buffer = SimpleInventory(1)
    private val output = SimpleInventory(1)

    init {
        with(rootPanel as WGridPanel) {
            val gridSlot = WItemSlot.of(grid, 0, CrafterBlockEntity.GRID_WIDTH, CrafterBlockEntity.GRID_HEIGHT)
                .apply { isModifiable = false }
            add(gridSlot, 0, 1)

            val resultSlot = WItemSlot.of(result, 0).apply { isModifiable = false }
            add(resultSlot, 4, 2)

            val bufferSlot = WItemSlot.of(buffer, 0).apply { isModifiable = false }
            add(bufferSlot, 8, 0)

            val outputSlot = WItemSlot.outputOf(output, 0).apply { isModifiable = false }
            add(outputSlot, 6, 2)

            add(createPlayerInventoryPanel(true), 0, 4)
        }
        rootPanel.validate(this)
    }

    private val subscriptions = blockEntity?.let {
        arrayOf(
            it.recipeUpdateEmitter.listen(this::onRecipeUpdated),
            it.outputUpdateEmitter.listen(this::onOutputUpdated)
        )
    }

    override fun close(player: PlayerEntity) {
        super.close(player)
        subscriptions?.close()
    }

    private fun onRecipeUpdated(update: RecipeUpdate) {
        val (newGrid, newResult) = update
        gridUpdateEmitter?.lock()?.use {
            for (idx in 0 until CrafterBlockEntity.GRID_SIZE) {
                if (ItemStack.areEqual(grid.getStack(idx), newGrid[idx])) {
                    continue
                }
                grid.setStack(idx, newGrid[idx].copy())
            }
            if (!ItemStack.areEqual(result.getStack(0), newResult)) {
                result.setStack(0, newResult.copy())
            }
        }
    }

    private fun onOutputUpdated(update: OutputUpdate) {
        val (newBuffer, newOutput) = update
        buffer.setStack(0, newBuffer.copy())
        output.setStack(0, newOutput.copy())
    }

    override fun onSlotClick(slotIndex: Int, button: Int, actionType: SlotActionType, player: PlayerEntity) {
        if (world.isClient) return
        slots.getOrNull(slotIndex)?.let { slot ->
            return when (slot.inventory) {
                grid   -> onGridClick(slot, actionType)
                output -> onOutputClick(slot, actionType)
                else   -> super.onSlotClick(slotIndex, button, actionType, player)
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

        logger.info("ignored onGridClick: #{} {}", slot.index, actionType)
    }

    private fun onOutputClick(slot: Slot, actionType: SlotActionType) {
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
    }

    private fun doCraft(target: Storage<ItemVariant>, maxAmount: Long, resource: ItemVariant, tx: Transaction) {
        logger.info("trying to move {} {} from {} to {}", maxAmount, toItemString(resource), crafter, target)

        val filter: Predicate<ItemVariant> =
            if (resource.isBlank) Predicate { true }
            else Predicate.isEqual(resource)

        val extracted = StorageUtil.move(crafter, target, filter, maxAmount, tx)
        logger.info("crafted {}", extracted)
    }
}
