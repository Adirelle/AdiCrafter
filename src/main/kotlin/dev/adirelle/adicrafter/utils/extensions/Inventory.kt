package dev.adirelle.adicrafter.utils.extensions

import dev.adirelle.adicrafter.utils.requireExactSize
import net.minecraft.inventory.Inventory
import net.minecraft.item.ItemStack

fun Inventory.copyFrom(list: List<ItemStack>) {
    requireExactSize(list, size()).forEachIndexed { index, stack ->
        this[index] = stack
    }
}

fun Inventory.iterator() =
    InventoryIterator(this)

operator fun Inventory.get(index: Int): ItemStack =
    getStack(index)

operator fun Inventory.set(index: Int, stack: ItemStack) =
    setStack(index, stack)

class InventoryIterator(private val inventory: Inventory) : Iterator<ItemStack> {

    private var idx = 0
    override fun hasNext() = idx < inventory.size()
    override fun next(): ItemStack = inventory.getStack(idx++)
}
