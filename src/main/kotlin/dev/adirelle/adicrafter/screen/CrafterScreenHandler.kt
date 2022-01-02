@file:Suppress("UnstableApiUsage")

package dev.adirelle.adicrafter.screen

import dev.adirelle.adicrafter.AdiCrafter.CRAFTER_SCREEN_HANDLER
import dev.adirelle.adicrafter.utils.extension.safeClose
import dev.adirelle.adicrafter.utils.inventory.api.ReadonlyInventory
import dev.adirelle.adicrafter.utils.lazyLogger
import io.github.cottonmc.cotton.gui.SyncedGuiDescription
import io.github.cottonmc.cotton.gui.widget.WGridPanel
import io.github.cottonmc.cotton.gui.widget.WItemSlot
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.inventory.Inventory
import net.minecraft.inventory.SimpleInventory
import net.minecraft.screen.slot.SlotActionType

class CrafterScreenHandler(
    syncId: Int,
    playerInventory: PlayerInventory,
    private val grid: Inventory = SimpleInventory(9),
    private val buffer: Inventory = SimpleInventory(1),
    private val result: Inventory = SimpleInventory(1)

) : SyncedGuiDescription(CRAFTER_SCREEN_HANDLER, syncId, playerInventory) {

    private val logger by lazyLogger()
    private val disposable = ArrayList<AutoCloseable>()

    init {
        with(rootPanel as WGridPanel) {
            add(WItemSlot.of(grid, 0, 3, 3), 0, 1)

            val resultSlot = WItemSlot.of(result, 0).apply { isModifiable = false }
            add(resultSlot, 4, 2)

            val bufferSlot = WItemSlot.of(buffer, 0).apply { isModifiable = false }
            add(bufferSlot, 8, 0)

            /*val outputSlot = WItemSlot.outputOf(output, 0).apply { isInsertingAllowed = false }
            add(outputSlot, 6, 2)*/

            add(createPlayerInventoryPanel(true), 0, 4, 9, 4)
        }
        rootPanel.validate(this)

        listenTo(grid) { logger.info("grid({}) changed: {}", grid::class.java.simpleName, grid) }
        listenTo(result) { logger.info("result({}) changed: {}", grid::class.java.simpleName, result) }
        listenTo(buffer) { logger.info("buffer({}) changed: {}", grid::class.java.simpleName, buffer) }
    }

    override fun onSlotClick(slotIndex: Int, button: Int, actionType: SlotActionType?, player: PlayerEntity?) {
        if (0 <= slotIndex && slotIndex < slots.size) {
            val slot = slots[slotIndex]
            if (slot.inventory === grid) {
                logger.info("#{}: replacing {} by a copy of {}", slot.index, slot.stack.item, cursorStack.item)
                slot.stack = cursorStack.copy().apply { count = 1 }
                return
            }
        }
        super.onSlotClick(slotIndex, button, actionType, player)
    }

    override fun close(player: PlayerEntity?) {
        disposable.safeClose()
        super.close(player)
    }

    private fun listenTo(target: Inventory, block: () -> Unit) {
        when (target) {
            is SimpleInventory   -> target.addListener { block() }
            is ReadonlyInventory -> disposable.add(target.onContentChanged { block() })
        }
    }
}
