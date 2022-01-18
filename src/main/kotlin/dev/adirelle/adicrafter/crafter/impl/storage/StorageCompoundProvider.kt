@file:Suppress("UnstableApiUsage")

package dev.adirelle.adicrafter.crafter.impl.storage

import dev.adirelle.adicrafter.crafter.api.storage.ResourceType
import dev.adirelle.adicrafter.crafter.api.storage.SingleTypeStorageProvider
import dev.adirelle.adicrafter.crafter.api.storage.StorageProvider
import net.fabricmc.fabric.api.transfer.v1.storage.Storage
import net.fabricmc.fabric.api.transfer.v1.storage.TransferVariant

class StorageCompoundProvider(private val providers: Map<ResourceType<*>, SingleTypeStorageProvider<*>>) :
    StorageProvider {

    override fun <T : TransferVariant<*>> getStorage(type: ResourceType<T>): Storage<T> {
        val storage = providers[type]?.getStorage()
            ?: throw IllegalArgumentException("unsupported resource type: $type")
        @Suppress("UNCHECKED_CAST")
        return storage as Storage<T>
    }
}
