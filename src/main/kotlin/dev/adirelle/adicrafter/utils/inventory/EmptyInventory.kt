package dev.adirelle.adicrafter.utils.inventory

import dev.adirelle.adicrafter.utils.ReadonlyInventory
import net.minecraft.item.ItemStack

object EmptyInventory : ReadonlyInventory {

    override fun size() = 0
    override fun isEmpty() = true
    override fun getStack(slot: Int): ItemStack = ItemStack.EMPTY
}
