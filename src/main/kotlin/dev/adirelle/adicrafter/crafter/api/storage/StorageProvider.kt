@file:Suppress("UnstableApiUsage")

package dev.adirelle.adicrafter.crafter.api.storage

import dev.adirelle.adicrafter.utils.storage.NullStorage
import net.fabricmc.fabric.api.transfer.v1.storage.Storage
import net.fabricmc.fabric.api.transfer.v1.storage.TransferVariant

interface StorageProvider {

    fun <T : TransferVariant<*>> getStorage(type: ResourceType<T>): Storage<T>

    fun <T : TransferVariant<*>> getStorage(resource: T) =
        getStorage(ResourceType.of(resource))

    companion object EMPTY : StorageProvider {

        override fun <T : TransferVariant<*>> getStorage(type: ResourceType<T>): Storage<T> =
            NullStorage(type.blank)
    }
}
