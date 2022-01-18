package dev.adirelle.adicrafter.utils

import net.minecraft.entity.player.PlayerEntity
import net.minecraft.inventory.Inventory
import net.minecraft.item.ItemStack

interface ReadonlyInventory : Inventory {

    override fun clear() {}
    override fun removeStack(slot: Int, amount: Int): ItemStack = ItemStack.EMPTY
    override fun removeStack(slot: Int): ItemStack = ItemStack.EMPTY
    override fun setStack(slot: Int, stack: ItemStack) {}
    override fun markDirty() {}
    override fun canPlayerUse(player: PlayerEntity) = true
}
