package dev.adirelle.adicrafter.utils.inventory

import net.minecraft.entity.player.PlayerEntity
import net.minecraft.inventory.SidedInventory
import net.minecraft.item.ItemStack
import net.minecraft.util.math.Direction

open class ArrayInventoryAdapter(
    private val stacks: Array<ItemStack>
) : SidedInventory, Iterable<ItemStack> {

    private val _availableSlots by lazy { IntArray(stacks.size) { it } }

    val onDirty: () -> Unit = {}

    override fun size() = stacks.size
    override fun isEmpty(): Boolean = stacks.isEmpty() || stacks.all { it.isEmpty }

    override fun markDirty() {
        onDirty()
    }

    override fun canPlayerUse(player: PlayerEntity) = true
    override fun getAvailableSlots(side: Direction?) = _availableSlots
    override fun canInsert(slot: Int, stack: ItemStack, dir: Direction?) = isValid(slot, stack)
    override fun canExtract(slot: Int, stack: ItemStack, dir: Direction?) = isValid(slot, stack)
    override fun iterator(): Iterator<ItemStack> = stacks.iterator()

    override fun clear() {
        stacks.indices.forEach { stacks[it] = ItemStack.EMPTY }
        markDirty()
    }

    private fun checkSlot(slot: Int): Int {
        require(slot in 0 until stacks.size) { "slot index out of bounds" }
        return slot
    }

    override fun getStack(slot: Int) =
        stacks[checkSlot(slot)]

    override fun removeStack(slot: Int, amount: Int): ItemStack {
        val removed = stacks[checkSlot(slot)].split(amount)
        markDirty()
        return removed
    }

    override fun removeStack(slot: Int): ItemStack {
        checkSlot(slot)
        val removed = stacks[slot]
        stacks[slot] = ItemStack.EMPTY
        markDirty()
        return removed
    }

    override fun setStack(slot: Int, stack: ItemStack) {
        checkSlot(slot)
        stacks[slot] = stack
        markDirty()
    }

    companion object {

        fun create(vararg stacks: ItemStack) =
            ArrayInventoryAdapter(arrayOf(*stacks))

        fun create(stacks: Iterable<ItemStack>) =
            ArrayInventoryAdapter(stacks.toList().toTypedArray())
    }

}
