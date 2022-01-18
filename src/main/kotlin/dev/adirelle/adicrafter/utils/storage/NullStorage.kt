@file:Suppress("UnstableApiUsage")

package dev.adirelle.adicrafter.utils.storage

import net.fabricmc.fabric.api.transfer.v1.storage.TransferVariant
import net.fabricmc.fabric.api.transfer.v1.storage.base.InsertionOnlyStorage
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext

class NullStorage<T : TransferVariant<*>>(private val blank: T) : SingleViewStorage<T>, InsertionOnlyStorage<T> {

    init {
        require(blank.isBlank)
    }

    override fun extract(resource: T, maxAmount: Long, transaction: TransactionContext): Long =
        super.extract(resource, maxAmount, transaction)

    override fun isResourceBlank() = true
    override fun getResource() = blank
    override fun getAmount() = 0L
    override fun getCapacity() = 0L
}
