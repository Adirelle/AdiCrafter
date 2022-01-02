@file:Suppress("UnstableApiUsage")

package dev.adirelle.adicrafter.utils.storage.slotarray.impl

import dev.adirelle.adicrafter.utils.extension.EMPTY_ITEM_AMOUNT
import dev.adirelle.adicrafter.utils.extension.ItemAmount
import dev.adirelle.adicrafter.utils.extension.transactional
import dev.adirelle.adicrafter.utils.lazyLogger
import dev.adirelle.adicrafter.utils.storage.slotarray.api.MutableSlotArray
import dev.adirelle.adicrafter.utils.storage.slotarray.api.SlotArray
import dev.adirelle.adicrafter.utils.storage.slotarray.impl.MutableSlotArrayImpl.Snapshot
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext
import net.fabricmc.fabric.api.transfer.v1.transaction.base.SnapshotParticipant

class MutableSlotArrayImpl(
    private val backing: SlotArrayImpl,
    private val txc: TransactionContext?
) : SnapshotParticipant<Snapshot>(),
    SlotArray by backing,
    MutableSlotArray {

    private val logger by lazyLogger()

    private var dirty = false

    override fun createSnapshot() = Snapshot(backing.slots.copyOf(), dirty)

    override fun readSnapshot(snapshot: Snapshot) {
        backing.slots = snapshot.slots
        dirty = snapshot.dirty
    }

    override fun onFinalCommit() {
        if (dirty) {
            logger.info("dirty at the end of transaction, dispatching, {}", this)
            dirty = false
            backing.versionFactory.next()
        }
    }

    override operator fun set(slot: Int, amount: ItemAmount) {
        if (slot !in indices) {
            logger.atWarn().withLocation().log("write access to slot out of bounds: {}/{}", slot, size)
            return
        }
        if (backing.slots[slot] == amount) return
        transactional(txc) { tx ->
            updateSnapshots(tx)
            backing.slots[slot] = amount
            dirty = true
        }
    }

    override fun clear() {
        if (isEmpty()) return
        transactional(txc) { tx ->
            updateSnapshots(tx)
            backing.slots.fill(EMPTY_ITEM_AMOUNT)
            dirty = true
        }
    }

    override fun toString() = StringBuilder().apply {
        if (dirty) {
            append("*")
        }
        append(backing.toString())
        append("{")
        append(txc?.nestingDepth() ?: -1)
        append("}")
    }.toString()

    override fun iterator() = Iterator()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MutableSlotArrayImpl

        if (backing != other.backing) return false

        return true
    }

    override fun hashCode(): Int {
        return backing.hashCode()
    }

    inner class Iterator : MutableListIterator<ItemAmount> {

        var cursor = 0

        override fun hasNext() = cursor < size
        override fun nextIndex() = cursor + 1
        override fun next() = get(cursor++)

        override fun hasPrevious() = cursor > 0
        override fun previousIndex() = cursor - 1
        override fun previous() = get(--cursor)

        override fun set(element: ItemAmount) = set(cursor, element)
        override fun add(element: ItemAmount) = throw UnsupportedOperationException("cannot slot to MutableArraySlot")
        override fun remove() = throw UnsupportedOperationException("cannot remove slot from MutableArraySlot")
    }

    data class Snapshot(val slots: Array<ItemAmount>, val dirty: Boolean) {

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Snapshot

            if (!slots.contentEquals(other.slots)) return false
            if (dirty != other.dirty) return false

            return true
        }

        override fun hashCode(): Int {
            var result = slots.contentHashCode()
            result = 31 * result + dirty.hashCode()
            return result
        }
    }

}
