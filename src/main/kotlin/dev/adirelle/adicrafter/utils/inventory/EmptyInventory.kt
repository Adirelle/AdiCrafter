package dev.adirelle.adicrafter.utils.inventory

import dev.adirelle.adicrafter.utils.Listenable.Listener
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ItemStack

object EmptyInventory : ListenableInventory {

    override fun addListener(listener: Listener) {}
    override fun removeListener(listener: Listener) {}
    override fun clear() {}
    override fun size() = 1
    override fun isEmpty() = true
    override fun getStack(slot: Int): ItemStack = ItemStack.EMPTY
    override fun removeStack(slot: Int, amount: Int): ItemStack = ItemStack.EMPTY
    override fun removeStack(slot: Int): ItemStack = ItemStack.EMPTY
    override fun setStack(slot: Int, stack: ItemStack) {}
    override fun markDirty() {}
    override fun canPlayerUse(player: PlayerEntity?) = true
}
