package dev.adirelle.adicrafter.utils.extension

import net.minecraft.inventory.Inventory
import net.minecraft.item.ItemStack

fun Inventory.toArray(): Array<ItemStack> =
    Array(size()) { idx -> getStack(idx).copy() }

fun Inventory.copyFrom(items: Array<ItemStack>) {
    if (size() != items.size) {
        throw UnsupportedOperationException("cannot copy inventories of different size")
    }
    for (index in 0 until size()) {
        val stackFrom = items[index]
        val stackTo = getStack(index)
        if (ItemStack.areEqual(stackFrom, stackTo)) {
            continue
        }
        setStack(index, stackFrom.copy())
    }
}
