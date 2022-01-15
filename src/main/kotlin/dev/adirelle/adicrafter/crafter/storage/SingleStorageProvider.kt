@file:Suppress("UnstableApiUsage")

package dev.adirelle.adicrafter.crafter.storage

import net.fabricmc.fabric.api.transfer.v1.storage.Storage
import net.fabricmc.fabric.api.transfer.v1.storage.TransferVariant

class SingleStorageProvider<T, V : TransferVariant<T>>(
    override val type: ResourceType<T>,
    private val storage: Storage<V>
) : StorageCompoundProvider.SingleTypeStorageProvider<T, V> {

    override fun getStorage() = storage
}
