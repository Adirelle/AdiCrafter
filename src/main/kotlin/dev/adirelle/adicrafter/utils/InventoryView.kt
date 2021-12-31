package dev.adirelle.adicrafter.utils

import net.minecraft.entity.player.PlayerEntity
import net.minecraft.inventory.Inventory
import net.minecraft.item.ItemStack

open class InventoryView(private val stacks: MutableList<ItemStack>) : Inventory {

    override fun clear() {
        stacks.indices.forEach { stacks[it] = ItemStack.EMPTY }
        markDirty()
    }

    override fun size() =
        stacks.size

    override fun isEmpty() =
        !stacks.any { it != ItemStack.EMPTY && !it.isEmpty }

    override fun getStack(slot: Int) =
        stacks[slot]

    override fun removeStack(slot: Int, amount: Int): ItemStack {
        val split = stacks[slot].split(amount)!!
        markDirty()
        return split
    }

    override fun removeStack(slot: Int): ItemStack {
        val old = stacks[slot]
        if (!old.isEmpty) {
            stacks[slot] = ItemStack.EMPTY
            markDirty()
        }
        return old
    }

    override fun setStack(slot: Int, stack: ItemStack) {
        if (stacks[slot] != stack) {
            markDirty()
        }
        stacks[slot] = stack
    }

    override fun markDirty() {}

    override fun canPlayerUse(player: PlayerEntity?) = true
}
