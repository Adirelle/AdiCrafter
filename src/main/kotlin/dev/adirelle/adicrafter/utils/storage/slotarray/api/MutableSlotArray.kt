@file:Suppress("UnstableApiUsage")

package dev.adirelle.adicrafter.utils.storage.slotarray.api

import dev.adirelle.adicrafter.utils.extension.ItemAmount

interface MutableSlotArray : SlotArray, MutableIterable<ItemAmount> {

    operator fun set(slot: Int, amount: ItemAmount)
    fun clear()
    override operator fun iterator(): MutableListIterator<ItemAmount>
}
