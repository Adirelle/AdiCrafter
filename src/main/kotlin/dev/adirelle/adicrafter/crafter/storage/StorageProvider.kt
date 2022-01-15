@file:Suppress("UnstableApiUsage")

package dev.adirelle.adicrafter.crafter.storage

import net.fabricmc.fabric.api.transfer.v1.storage.Storage
import net.fabricmc.fabric.api.transfer.v1.storage.TransferVariant

interface StorageProvider {

    fun <T> getStorage(type: ResourceType<T>): Storage<TransferVariant<T>>

    fun <T> getStorage(resource: TransferVariant<T>) =
        getStorage(ResourceType.of(resource))
}
