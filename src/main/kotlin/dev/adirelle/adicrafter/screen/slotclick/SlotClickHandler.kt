package dev.adirelle.adicrafter.screen.slotclick

import net.minecraft.entity.player.PlayerEntity
import net.minecraft.inventory.Inventory
import net.minecraft.screen.slot.Slot
import net.minecraft.screen.slot.SlotActionType

interface SlotClickHandler : Inventory {

    fun handleSlotClick(slot: Slot, button: Int, actionType: SlotActionType, player: PlayerEntity): Boolean {
        return false
    }

    fun Inventory.handleSlotClick(slot: Slot, button: Int, actionType: SlotActionType, player: PlayerEntity) =
        (this as? SlotClickHandler)?.handleSlotClick(slot, button, actionType, player) ?: false

    abstract class Abstract<T : Inventory>(
        protected val backing: T
    ) :
        Inventory by backing,
        SlotClickHandler
}

