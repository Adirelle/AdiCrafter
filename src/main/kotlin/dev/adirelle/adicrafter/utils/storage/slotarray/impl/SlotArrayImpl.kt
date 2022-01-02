@file:Suppress("UnstableApiUsage")

package dev.adirelle.adicrafter.utils.storage.slotarray.impl

import dev.adirelle.adicrafter.utils.extension.EMPTY_ITEM_AMOUNT
import dev.adirelle.adicrafter.utils.extension.ItemAmount
import dev.adirelle.adicrafter.utils.extension.isEmpty
import dev.adirelle.adicrafter.utils.extension.transactional
import dev.adirelle.adicrafter.utils.lazyLogger
import dev.adirelle.adicrafter.utils.storage.slotarray.api.MutableSlotArray
import dev.adirelle.adicrafter.utils.storage.slotarray.api.SlotArray
import dev.adirelle.adicrafter.utils.storage.slotarray.api.SlotArray.Updater
import dev.adirelle.adicrafter.utils.storage.slotarray.api.VersionFactory
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext

class SlotArrayImpl(
    override val size: Int,
    factory: (Int) -> ItemAmount = { EMPTY_ITEM_AMOUNT }
) :
    SlotArray {

    private val logger by lazyLogger()

    internal var slots = Array(size, factory)

    internal val versionFactory = VersionFactory.create()
    override fun getVersion() = versionFactory.value
    override val onContentChanged by versionFactory::onChanged

    override val indices = IntRange(0, size - 1)

    override fun isEmpty() = slots.all { it.isEmpty() }

    override fun iterator() = Iterator()

    override operator fun get(slot: Int): ItemAmount {
        if (slot !in indices) {
            logger.atWarn().withLocation().log("read access to slot out of bounds: {}/{}", slot, size)
            return EMPTY_ITEM_AMOUNT
        }
        return slots[slot]
    }

    override fun asMutable(txc: TransactionContext?): MutableSlotArray =
        MutableSlotArrayImpl(this, txc)

    override fun <T> update(txc: TransactionContext?, block: Updater<T>): T =
        transactional(txc) { tx -> block.update(asMutable(tx), tx) }

    override fun toString() =
        slots.joinToString(", ", "[", "]") {
            when (it.amount) {
                0L   -> "-"
                1L   -> it.resource.item.toString()
                else -> "%s x %d".format(it.resource.item, it.amount)
            }
        }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SlotArrayImpl

        if (size != other.size) return false
        if (!slots.contentEquals(other.slots)) return false
        if (versionFactory.value != other.versionFactory.value) return false

        return true
    }

    override fun hashCode(): Int {
        var result = size
        result = 31 * result + slots.contentHashCode()
        result = 31 * result + versionFactory.value.hashCode()
        return result
    }

    inner class Iterator : ListIterator<ItemAmount> {

        private var cursor = 0

        override fun hasNext() = cursor < size
        override fun nextIndex() = cursor + 1
        override fun next() = get(cursor++)

        override fun hasPrevious() = cursor > 0
        override fun previousIndex() = cursor - 1
        override fun previous() = get(--cursor)
    }
}
