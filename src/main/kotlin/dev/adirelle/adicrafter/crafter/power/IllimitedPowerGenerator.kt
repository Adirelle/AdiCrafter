@file:Suppress("UnstableApiUsage")

package dev.adirelle.adicrafter.crafter.power

import dev.adirelle.adicrafter.crafter.power.PowerVariant.INSTANCE
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleViewIterator
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext
import net.minecraft.nbt.NbtCompound

class IllimitedPowerGenerator : PowerGenerator {

    override var onUpdate: () -> Unit = {}

    override fun insert(resource: PowerVariant, maxAmount: Long, transaction: TransactionContext) = 0L
    override fun extract(resource: PowerVariant, maxAmount: Long, transaction: TransactionContext) = maxAmount
    override fun iterator(transaction: TransactionContext): MutableIterator<StorageView<PowerVariant>> =
        SingleViewIterator.create(this, transaction)

    override fun isResourceBlank() = false
    override fun getResource() = INSTANCE
    override fun getAmount() = Long.MAX_VALUE
    override fun getCapacity() = Long.MAX_VALUE

    override fun readFromNbt(nbt: NbtCompound) {}
    override fun writeToNbt(nbt: NbtCompound) {}
}
