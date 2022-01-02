package dev.adirelle.adicrafter.utils.extension

import net.minecraft.inventory.Inventory
import net.minecraft.item.ItemStack

fun Inventory.toArray(): Array<ItemStack> =
    Array(size()) { idx -> getStack(idx).copy() }

fun Inventory.copyFrom(items: Array<ItemStack>) {
    for (index in 0 until size()) {
        setStack(
            index,
            if (index < items.size) items[index].copy()
            else ItemStack.EMPTY
        )
    }
}
