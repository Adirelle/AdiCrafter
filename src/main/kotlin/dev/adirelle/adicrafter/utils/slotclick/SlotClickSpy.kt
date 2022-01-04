package dev.adirelle.adicrafter.utils.slotclick

import dev.adirelle.adicrafter.utils.lazyLogger
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.inventory.Inventory
import net.minecraft.screen.slot.Slot
import net.minecraft.screen.slot.SlotActionType

class SlotClickSpy<T : Inventory>(backing: T, name: String) :
    SlotClickHandler.Abstract<T>(backing) {

    val logger by lazyLogger(name)

    override fun handleSlotClick(
        slot: Slot,
        button: Int,
        actionType: SlotActionType,
        player: PlayerEntity
    ): Boolean {
        logger.info("slot click: idx={}, button={}, action={}", slot.index, button, actionType)
        return backing.handleSlotClick(slot, button, actionType, player)
    }
}
