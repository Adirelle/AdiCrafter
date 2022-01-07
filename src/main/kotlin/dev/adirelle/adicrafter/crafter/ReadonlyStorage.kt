@file:Suppress("UnstableApiUsage")

package dev.adirelle.adicrafter.crafter

import net.fabricmc.fabric.api.transfer.v1.storage.Storage
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext

interface ReadonlyStorage<T> : Storage<T> {

    override fun insert(resource: T, maxAmount: Long, transaction: TransactionContext?) = 0L
    override fun supportsInsertion() = false
}
