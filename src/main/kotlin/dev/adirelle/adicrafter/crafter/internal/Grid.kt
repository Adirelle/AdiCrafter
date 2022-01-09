@file:Suppress("UnstableApiUsage")

package dev.adirelle.adicrafter.crafter.internal

import dev.adirelle.adicrafter.utils.areEqual
import dev.adirelle.adicrafter.utils.extension.toAmount
import dev.adirelle.adicrafter.utils.general.extensions.EMPTY_ITEM_AMOUNT
import dev.adirelle.adicrafter.utils.general.extensions.resourceAmountFromNbt
import dev.adirelle.adicrafter.utils.general.extensions.toNbt
import dev.adirelle.adicrafter.utils.minecraft.NbtPersistable
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant
import net.fabricmc.fabric.api.transfer.v1.storage.base.ResourceAmount
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NbtList

class Grid private constructor(
    val slots: List<ResourceAmount<ItemVariant>>
) : NbtPersistable<NbtList>, List<ResourceAmount<ItemVariant>> by slots {

    companion object {

        const val WIDTH = 3
        const val HEIGHT = 3
        const val SIZE = WIDTH * HEIGHT

        val EMPTY = Grid(List(SIZE) { EMPTY_ITEM_AMOUNT })

        fun fromNbt(nbt: NbtList): Grid =
            if (nbt.size == SIZE)
                Grid(List(SIZE) { resourceAmountFromNbt(nbt.getCompound(it)) })
            else
                EMPTY

        fun copyOf(other: List<ItemStack>): Grid {
            if (other.size != SIZE) {
                throw IndexOutOfBoundsException("expected a list of $SIZE elements")
            }
            return Grid(List(SIZE, { other[it].toAmount() }))
        }
    }

    override fun toNbt() =
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
}
