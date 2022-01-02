@file:Suppress("UnstableApiUsage")

package dev.adirelle.adicrafter.utils.storage

import dev.adirelle.adicrafter.utils.extension.*
import dev.adirelle.adicrafter.utils.inventory.api.ReadonlyInventory
import dev.adirelle.adicrafter.utils.lazyLogger
import dev.adirelle.adicrafter.utils.storage.slotarray.api.MutableSlotArray
import dev.adirelle.adicrafter.utils.storage.slotarray.api.SlotArray
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant
import net.fabricmc.fabric.api.transfer.v1.storage.Storage
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.inventory.SidedInventory
import net.minecraft.item.ItemStack
import net.minecraft.item.ItemStack.EMPTY
import net.minecraft.nbt.NbtCompound
import net.minecraft.nbt.NbtList
import net.minecraft.util.math.Direction
import toAmount
import java.lang.Long.min

@Suppress("UnstableApiUsage")
class SimpleInventoryStorage constructor(size: Int) : Storage<ItemVariant>, ReadonlyInventory {

    private val slots: SlotArray = SlotArray.of(size)

    val logger by lazyLogger()

    companion object {

        private const val SLOT_NBT_KEY = "Slot"
    }

    override val onContentChanged by slots::onContentChanged
    override fun getVersion() = slots.getVersion()

    override fun insert(resource: ItemVariant, maxAmount: Long, tx: TransactionContext?): Long =
        if (resource.isBlank || maxAmount < 1) 0
        else slots.update(tx) { mut, _ ->
            val stack = resource.toStack()
            val maxStackSize = stack.maxCount.toLong()
            val iterator = mut.iterator()
            var inserted = 0L
            while (inserted < maxAmount && iterator.hasNext()) {
                val slot = iterator.next()
                if (!slot.canCombineWith(stack)) continue
                val inserting = min(maxStackSize - slot.amount, maxAmount - inserted)
                if (inserting < 1) continue
                iterator.set(slot.addAmount(inserting))
                inserted += inserting
            }
            inserted
        }

    override fun extract(resource: ItemVariant, maxAmount: Long, tx: TransactionContext?): Long =
        if (resource.isBlank || maxAmount < 1) 0
        else slots.update(tx) { mut, _ ->
            var extracted = 0L
            val stack = resource.toStack()
            val iterator = mut.iterator()
            while (extracted < maxAmount && iterator.hasNext()) {
                val slot = iterator.next()
                if (slot.isEmpty() || !slot.canCombineWith(stack)) continue
                val extracting = min(slot.amount, maxAmount - extracted)
                iterator.set(slot.addAmount(-extracting))
                extracted += extracting
            }
            extracted
        }

    fun readNbt(nbt: NbtList) {
        slots.update(null) { mut, _ ->
            {
                mut.clear()
                nbt.filterIsInstance(NbtCompound::class.java)
                    .forEach { entryNbt ->
                        val slot = entryNbt.getInt(SLOT_NBT_KEY) - 1
                        val content = ItemStack.fromNbt(entryNbt)
                        if (slot in slots.indices) {
                            mut[slot] = content.toAmount()
                        }
                    }
            }
        }
    }

    fun writeNbt(nbt: NbtList): NbtList {
        nbt.addAll(
            slots.withIndex()
                .filter { !it.value.isEmpty() }
                .map { (index, content) ->
                    NbtCompound().also { entryNbt ->
                        entryNbt.putInt(SLOT_NBT_KEY, index + 1)
                        content.toStack().writeNbt(entryNbt)
                    }
                }
        )
        return nbt
    }

    override fun size() = slots.size
    override fun isEmpty() = slots.isEmpty()
    override fun getStack(slot: Int) = slots[slot].toStack()
    override fun canPlayerUse(player: PlayerEntity) = true

    private val availableSlots: IntArray by lazy { IntArray(size) { it } }
    override fun getAvailableSlots(side: Direction?) = availableSlots

    override fun toString() = slots.toString()

    override fun iterator(txc: TransactionContext?): MutableIterator<StorageView<ItemVariant>> =
        MutableViewIterator(slots.asMutable(Transaction.openNested(txc)))

    private class MutableViewIterator(val slots: MutableSlotArray) : MutableIterator<StorageView<ItemVariant>> {

        private var cursor = 0

        override fun hasNext() = cursor < slots.size
        override fun next() = SlotStorageView(cursor)
        override fun remove() {}

        private inner class SlotStorageView(val index: Int) : StorageView<ItemVariant> {

            override fun extract(resource: ItemVariant, maxAmount: Long, transaction: TransactionContext?): Long {
                if (resource.isBlank || maxAmount < 1) return 0
                val content = slots[index]
                if (content.resource != resource || content.amount < 1) return 0
                val extracted = min(maxAmount, content.amount)
                slots[index] = content.addAmount(-extracted)
                return extracted
            }

            override fun isResourceBlank() = slots[index].isEmpty()
            override fun getResource(): ItemVariant = slots[index].resource
            override fun getAmount() = slots[index].amount
            override fun getCapacity() = slots[index].maxStackAmount()
        }

    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SimpleInventoryStorage

        if (slots != other.slots) return false

        return true
    }

    override fun hashCode(): Int {
        return slots.hashCode()
    }

    fun asMutableInventory(txc: TransactionContext? = null): SidedInventory = InventoryView(slots.asMutable(txc))

    private class InventoryView(private val backing: MutableSlotArray) : SidedInventory {

        val logger by lazyLogger()

        override fun size() = backing.size
        override fun clear() = backing.clear()
        override fun isEmpty() = backing.isEmpty()
        override fun canPlayerUse(player: PlayerEntity) = true

        override fun getStack(slot: Int) = backing[slot].toStack()

        override fun removeStack(slot: Int, maxAmount: Int): ItemStack {
            val content = backing[slot]
            val removed = min(content.amount, maxAmount.toLong())
            if (removed == 0L) return EMPTY
            backing[slot] = content.addAmount(-removed)
            return content.toStack(removed.toInt())
        }

        override fun removeStack(slot: Int): ItemStack {
            val content = backing[slot]
            backing[slot] = EMPTY_ITEM_AMOUNT
            return content.toStack()
        }

        override fun setStack(slot: Int, stack: ItemStack) {
            backing[slot] = stack.toAmount()
        }

        override fun markDirty() {
            logger.info("{} marked as dirty: {}", this::class.java.simpleName, this)
        }

        override fun getAvailableSlots(side: Direction?) = IntArray(backing.size) { it }

        override fun canInsert(slot: Int, stack: ItemStack, dir: Direction?) = backing[slot].canCombineWith(stack)

        override fun canExtract(slot: Int, stack: ItemStack, dir: Direction?) = !backing[slot].isEmpty()

        override fun toString() = backing.toString()
    }
}
