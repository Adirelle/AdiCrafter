@file:Suppress("UnstableApiUsage")

package dev.adirelle.adicrafter.crafter.impl

import dev.adirelle.adicrafter.crafter.NbtKeys
import dev.adirelle.adicrafter.utils.NbtSerializable
import dev.adirelle.adicrafter.utils.areEqual
import dev.adirelle.adicrafter.utils.extensions.add
import dev.adirelle.adicrafter.utils.extensions.getStack
import dev.adirelle.adicrafter.utils.extensions.read
import dev.adirelle.adicrafter.utils.extensions.set
import dev.adirelle.adicrafter.utils.lazyLogger
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.inventory.Inventory
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NbtCompound
import net.minecraft.nbt.NbtList

class Grid private constructor(
    input: List<ItemStack>,
    private val onMarkedDirty: () -> Unit = {}
) : Iterable<ItemStack>, Inventory, NbtSerializable {

    private val logger by lazyLogger

    companion object {

        const val WIDTH = 3
        const val HEIGHT = 3
        const val SIZE = WIDTH * HEIGHT

        fun create(onMarkedDirty: () -> Unit = {}) =
            Grid(List(SIZE) { ItemStack.EMPTY }, onMarkedDirty)
    }

    private val slots = MutableList(input.size) { input[it] }

    override fun readFromNbt(nbt: NbtCompound) {
        nbt.read<NbtList>(NbtKeys.GRID) { items ->
            slots.indices.forEach { index ->
                slots[index] = items.getStack(index)
            }
            return
        }
        clear()
    }

    override fun writeToNbt(nbt: NbtCompound) {
        if (isEmpty) return
        nbt[NbtKeys.GRID] = NbtList().apply {
            slots.forEach { stack ->
                add(stack.item)
            }
        }
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
        !player.isSpectator && player.abilities.allowModifyWorld

}
