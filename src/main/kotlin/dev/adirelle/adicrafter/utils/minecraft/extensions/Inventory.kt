package dev.adirelle.adicrafter.utils.extension

import com.google.common.base.Preconditions
import dev.adirelle.adicrafter.utils.expectExactSize
import dev.adirelle.adicrafter.utils.general.memoize
import net.minecraft.inventory.Inventory
import net.minecraft.item.ItemStack
import net.minecraft.item.ItemStack.areEqual

val getInventoryListView = memoize(::InventoryListView)

fun Inventory.asList(): List<ItemStack> =
    getInventoryListView(this)

fun Inventory.toArray(): Array<ItemStack> =
    Array(size()) { idx -> getStack(idx).copy() }

fun Inventory.copyFrom(list: List<ItemStack>) {
    expectExactSize(list, size()).forEachIndexed { index, stack ->
        this[index] = stack
    }
}

fun Inventory.deepCopyFrom(list: List<ItemStack>) {
    expectExactSize(list, size()).forEachIndexed { index, stack ->
        this[index] = stack.copy()
    }
}

fun Inventory.asList(fromIndex: Int, toIndex: Int = fromIndex + 1): List<ItemStack> =
    InventoryListView(this, fromIndex, toIndex)

fun Inventory.iterator() =
    InventoryIterator(this)

operator fun Inventory.get(index: Int): ItemStack =
    getStack(index)

operator fun Inventory.set(index: Int, stack: ItemStack) =
    setStack(index, stack)

class InventoryIterator(private val inventory: Inventory) : Iterator<ItemStack> {

    private var idx = 0
    override fun hasNext() = idx < inventory.size()
    override fun next() = inventory.getStack(idx++)
}

class InventoryListView(
    private val inventory: Inventory,
    private val startIndex: Int = 0,
    endIndex: Int = inventory.size() - startIndex
) : List<ItemStack> {

    override val size = endIndex - startIndex

    override fun contains(element: ItemStack) =
        any { areEqual(element, it) }

    override fun containsAll(elements: Collection<ItemStack>) =
        elements.all { it in this }

    override fun get(index: Int): ItemStack =
        inventory.getStack(Preconditions.checkElementIndex(index, size) + startIndex)

    override fun isEmpty() = inventory.isEmpty

    override fun indexOf(element: ItemStack): Int {
        for (i in 0 until size) {
            if (areEqual(get(i), element)) {
                return i
            }
        }
        return -1
    }

    override fun lastIndexOf(element: ItemStack): Int {
        for (i in (size - 1) downTo 0) {
            if (areEqual(get(i), element)) {
                return i
            }
        }
        return -1
    }

    override fun iterator(): Iterator<ItemStack> =
        inventory.iterator()

    override fun listIterator(): ListIterator<ItemStack> =
        listIterator(0)

    override fun listIterator(index: Int): ListIterator<ItemStack> =
        InventoryListIterator(Preconditions.checkPositionIndex(index, size, "iterator starting index"))

    override fun subList(fromIndex: Int, toIndex: Int): List<ItemStack> {
        Preconditions.checkPositionIndexes(fromIndex, toIndex, size)
        return InventoryListView(inventory, fromIndex + startIndex, toIndex + startIndex)
    }

    private inner class InventoryListIterator(private var index: Int) : ListIterator<ItemStack> {

        override fun hasNext() = index < size
        override fun hasPrevious() = index > 0
        override fun nextIndex() = index
        override fun previousIndex() = index - 1

        override fun next(): ItemStack =
            if (index < size) inventory.getStack(index++)
            else throw IndexOutOfBoundsException("cannot iterate beyond the last element")

        override fun previous(): ItemStack =
            if (index > 0) inventory.getStack(--index)
            else throw IndexOutOfBoundsException("cannot iterate before the first element")
    }
}
