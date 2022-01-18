@file:Suppress("UnstableApiUsage")

package dev.adirelle.adicrafter.crafter.impl

import dev.adirelle.adicrafter.utils.areEqual
import dev.adirelle.adicrafter.utils.extensions.toNbt
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.inventory.Inventory
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NbtList

class Grid private constructor(
    input: List<ItemStack>,
    private val onMarkedDirty: () -> Unit = {}
) : Iterable<ItemStack>, Inventory {

    companion object {

        const val WIDTH = 3
        const val HEIGHT = 3
        const val SIZE = WIDTH * HEIGHT

        fun create(onMarkedDirty: () -> Unit = {}) =
            Grid(List(SIZE) { ItemStack.EMPTY }, onMarkedDirty)
    }

    private val slots = MutableList(input.size) { input[it] }

    fun readFromNbt(nbt: NbtList) {
        slots.indices.forEach { slot ->
            slots[slot] = ItemStack.fromNbt(nbt.getCompound(slot))
        }
    }

    fun toNbt() =
        NbtList().apply {
            slots.forEach { slot -> add(slot.toNbt()) }
        }

    fun asList(): List<ItemStack> =
        slots

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Grid

        return areEqual(slots, other.slots)
    }

    override fun hashCode() = slots.hashCode()

    override fun getMaxCountPerStack() = 1

    operator fun set(index: Int, stack: ItemStack) {
        if (slots[index].isOf(stack.item)) {
            return
        }
        slots[index] = if (stack.isEmpty) ItemStack.EMPTY else stack.copy().apply { count = 1 }
        markDirty()
    }

    operator fun get(index: Int) =
        slots[index]

    override fun iterator() =
        slots.iterator()

    override fun clear() {
        slots.indices.forEach { slots[it] = ItemStack.EMPTY }
        markDirty()
    }

    override fun size() =
        slots.size

    override fun isEmpty(): Boolean =
        slots.all { it.isEmpty }

    override fun getStack(slot: Int): ItemStack =
        get(slot).copy()

    override fun removeStack(slot: Int, amount: Int): ItemStack {
        set(slot, ItemStack.EMPTY)
        return ItemStack.EMPTY
    }

    override fun removeStack(slot: Int): ItemStack =
        removeStack(slot, 1)

    override fun setStack(slot: Int, stack: ItemStack) {
        set(slot, stack)
    }

    override fun markDirty() {
        onMarkedDirty()
    }

    override fun canPlayerUse(player: PlayerEntity) =
        true

}
