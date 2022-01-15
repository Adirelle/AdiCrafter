@file:Suppress("UnstableApiUsage")

package dev.adirelle.adicrafter.crafter.storage

import net.fabricmc.fabric.api.transfer.v1.storage.Storage
import net.fabricmc.fabric.api.transfer.v1.storage.TransferVariant

interface StorageProvider {

    fun <T, V : TransferVariant<T>> getStorage(type: ResourceType<T>): Storage<V>

    fun <T, V : TransferVariant<T>> getStorage(resource: V) =
        getStorage(ResourceType.of(resource))
}
