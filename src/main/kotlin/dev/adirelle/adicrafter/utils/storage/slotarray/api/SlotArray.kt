@file:Suppress("UnstableApiUsage")

package dev.adirelle.adicrafter.utils.storage.slotarray.api

import dev.adirelle.adicrafter.utils.callback.api.Callback
import dev.adirelle.adicrafter.utils.extension.EMPTY_ITEM_AMOUNT
import dev.adirelle.adicrafter.utils.extension.ItemAmount
import dev.adirelle.adicrafter.utils.storage.slotarray.impl.SlotArrayImpl
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext

interface SlotArray : Iterable<ItemAmount> {

    companion object {

        fun of(size: Int, factory: (Int) -> ItemAmount = { EMPTY_ITEM_AMOUNT }) = SlotArrayImpl(size, factory)

        fun copyOf(other: SlotArray) = of(other.size) { index -> other[index] }
        fun copyOf(list: List<ItemAmount>) = of(list.size) { index -> list[index] }
        fun copyOf(other: Iterable<ItemAmount>) = copyOf(other.toList())
    }

    val indices: IntRange
    val size: Int
    val onContentChanged: Callback<Long>

    fun getVersion(): Version

    fun isEmpty(): Boolean
    operator fun get(slot: Int): ItemAmount
    override operator fun iterator(): ListIterator<ItemAmount>

    fun asMutable(txc: TransactionContext? = null): MutableSlotArray

    fun <T> update(block: Updater<T>): T = update(null, block)
    fun <T> update(txc: TransactionContext?, block: Updater<T>): T

    fun interface Updater<out T> {

        fun update(slotArray: MutableSlotArray, transaction: Transaction): T
    }
}
