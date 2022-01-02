package dev.adirelle.adicrafter.utils.inventory.impl

import dev.adirelle.adicrafter.utils.inventory.api.StackDisplayInventory
import dev.adirelle.adicrafter.utils.storage.slotarray.api.VersionFactory
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ItemStack
import net.minecraft.util.math.Direction

class StackDisplayInventoryImpl(initialStack: ItemStack) : StackDisplayInventory {

    private val versionFactory = VersionFactory.create()
    override fun getVersion() = versionFactory.value
    override val onContentChanged by versionFactory::onChanged

    override var stack: ItemStack = initialStack
        set(value) {
            if (field != value) {
                field = value
                versionFactory.next()
            }
        }

    override fun size() = 1
    override fun isEmpty() = stack.isEmpty
    override fun getStack(slot: Int): ItemStack = stack.copy()
    override fun canPlayerUse(player: PlayerEntity) = false
    override fun getAvailableSlots(side: Direction) = intArrayOf(0)

    override fun toString() = stack.toString()
}
