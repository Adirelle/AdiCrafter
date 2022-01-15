@file:Suppress("UnstableApiUsage")

package dev.adirelle.adicrafter.crafter.storage

import net.fabricmc.fabric.api.transfer.v1.storage.Storage
import net.fabricmc.fabric.api.transfer.v1.storage.TransferVariant

class StorageCompoundProvider(providers: Iterable<SingleTypeStorageProvider<*>>) : StorageProvider {

    private val providerMap = buildMap {
        providers.forEach { provider ->
            put(provider.type, provider)
        }
    }

    override fun <T> getStorage(type: ResourceType<T>): Storage<TransferVariant<T>> =
        providerMap[type]?.getStorage(type)
            ?: throw IllegalArgumentException("unsupported resource type: $type")

    companion object {

        fun of(vararg providers: SingleTypeStorageProvider<*>) =
            StorageCompoundProvider(providers.asIterable())
    }
}
