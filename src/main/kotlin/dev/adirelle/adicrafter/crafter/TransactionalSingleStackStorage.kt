@file:Suppress("UnstableApiUsage")

package dev.adirelle.adicrafter.crafter

import dev.adirelle.adicrafter.utils.extension.canCombineWith
import dev.adirelle.adicrafter.utils.general.lazyLogger
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext
import net.fabricmc.fabric.api.transfer.v1.transaction.base.SnapshotParticipant
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NbtCompound
import kotlin.math.min

open class TransactionalSingleStackStorage :
    SingleSlotStorage<ItemVariant>,
    SnapshotParticipant<TransactionalSingleStackStorage.State>() {

    private val logger by lazyLogger

    companion object {

        private const val VARIANT_NBT_KEY = "Variant"
        private const val COUNT_NBT_KEY = "Count"

    }

    private var variant: ItemVariant = ItemVariant.blank()
    private var count = 0L
    private var changed = false

    fun readNbt(nbt: NbtCompound) {
        variant = ItemVariant.fromNbt(nbt.getCompound(VARIANT_NBT_KEY))
        count = nbt.getLong(COUNT_NBT_KEY)
        changed = false
    }

    fun writeNbt(nbt: NbtCompound) {
        nbt.put(VARIANT_NBT_KEY, variant.toNbt())
        nbt.putLong(COUNT_NBT_KEY, count)
    }

    fun toStack(): ItemStack =
        variant.toStack(count.toInt())

    override fun insert(resource: ItemVariant, maxAmount: Long, txc: TransactionContext?): Long =
        if (resource.isBlank || count == capacity || maxAmount < 1 || !resource.canCombineWith(variant)) 0L
        else {
            val inserted = min(capacity - count, maxAmount)
            updateSnapshots(txc)
            count += inserted
            changed = true
            logger.info("inserted {} into buffer", inserted)
            inserted
        }

    override fun extract(resource: ItemVariant, maxAmount: Long, txc: TransactionContext?): Long =
        if (isResourceBlank || maxAmount < 1 || !resource.canCombineWith(variant)) 0L
        else {
            val extracted = min(count, maxAmount)
            updateSnapshots(txc)
            count -= extracted
            changed = true
            logger.info("extracted {} from buffer", extracted)
            extracted
        }

    override fun isResourceBlank() = count == 0L || variant.isBlank
    override fun getResource() = variant
    override fun getCapacity() = variant.item.maxCount.toLong()
    override fun getAmount() = count

    override fun createSnapshot() =
        State(variant, count, changed)

    override fun readSnapshot(snapshot: State) {
        variant = snapshot.variant
        count = snapshot.count
        changed = snapshot.changed
    }

    override fun onFinalCommit() {
        if (!changed) return
        changed = false
        markDirty()
    }

    protected open fun markDirty() {}

    data class State(val variant: ItemVariant, val count: Long, val changed: Boolean)
}
