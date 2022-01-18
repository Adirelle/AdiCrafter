@file:Suppress("UnstableApiUsage")

package dev.adirelle.adicrafter.utils.storage

import net.fabricmc.fabric.api.transfer.v1.storage.StorageView
import net.fabricmc.fabric.api.transfer.v1.storage.TransferVariant
import net.fabricmc.fabric.api.transfer.v1.storage.base.ExtractionOnlyStorage
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext

interface SingleViewStorage<T : TransferVariant<*>> : SingleSlotStorage<T>, ExtractionOnlyStorage<T> {

    override fun exactView(transaction: TransactionContext, resource: T): StorageView<T>? =
        this.takeIf { this.resource == resource }
}
