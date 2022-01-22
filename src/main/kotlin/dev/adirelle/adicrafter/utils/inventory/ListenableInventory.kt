package dev.adirelle.adicrafter.utils.inventory

import dev.adirelle.adicrafter.utils.Listenable
import dev.adirelle.adicrafter.utils.Listenable.Listener
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.inventory.Inventory
import net.minecraft.item.ItemStack

interface ListenableInventory : Inventory, Listenable {

    companion object EMPTY : ListenableInventory {

        override fun clear() {}
        override fun size() = 0
        override fun isEmpty() = true
        override fun getStack(slot: Int): ItemStack = ItemStack.EMPTY
        override fun removeStack(slot: Int, amount: Int): ItemStack = ItemStack.EMPTY
        override fun removeStack(slot: Int): ItemStack = ItemStack.EMPTY
        override fun setStack(slot: Int, stack: ItemStack) {}
        override fun markDirty() {}
        override fun canPlayerUse(player: PlayerEntity?) = true
        override fun addListener(listener: Listener) {}
        override fun removeListener(listener: Listener) {}
    }
}
