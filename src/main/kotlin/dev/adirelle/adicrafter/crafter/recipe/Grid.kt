@file:Suppress("UnstableApiUsage")

package dev.adirelle.adicrafter.crafter.recipe

import dev.adirelle.adicrafter.utils.areEqual
import dev.adirelle.adicrafter.utils.extensions.toNbt
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NbtList

class Grid private constructor(
    input: List<ItemStack>
) : Iterable<ItemStack> {

    private val slots = MutableList(input.size) { input[it] }

    val size by slots::size

    companion object {

        const val WIDTH = 3
        const val HEIGHT = 3
        const val SIZE = WIDTH * HEIGHT

        fun empty() =
            Grid(List(SIZE) { ItemStack.EMPTY })

        fun fromNbt(nbt: NbtList): Grid =
            if (nbt.size == SIZE)
                Grid(List(SIZE) { ItemStack.fromNbt(nbt.getCompound(it)) })
            else
                empty()

        fun copyOf(other: List<ItemStack>): Grid {
            if (other.size != SIZE) {
                throw IndexOutOfBoundsException("expected a list of $SIZE elements")
            }
            return Grid(List(SIZE) { other[it].copy().apply { count = 1 } })
        }
    }

    fun toNbt() =
        NbtList().apply {
            slots.forEach { slot -> add(slot.toNbt()) }
        }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Grid

        return areEqual(slots, other.slots)
    }

    override fun hashCode() = slots.hashCode()

    operator fun set(index: Int, stack: ItemStack) {
        slots[index] = stack.copy().apply { count = 1 }
    }

    operator fun get(index: Int) =
        slots[index]

    override fun iterator() =
        slots.iterator()

    fun asList(): List<ItemStack> = slots
}
