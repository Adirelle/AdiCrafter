@file:Suppress("UnstableApiUsage")

package dev.adirelle.adicrafter.crafter.storage

import net.fabricmc.fabric.api.transfer.v1.storage.Storage
import net.fabricmc.fabric.api.transfer.v1.storage.TransferVariant

class StorageCompoundProvider(providers: Iterable<SingleTypeStorageProvider<*, *>>) : StorageProvider {

    private val providerMap = buildMap {
        providers.forEach { provider ->
            put(provider.type, provider)
        }
    }

    override fun <T, V : TransferVariant<T>> getStorage(type: ResourceType<T>): Storage<V> {
        val storage = providerMap[type]?.getStorage()
            ?: throw IllegalArgumentException("unsupported resource type: $type")
        @Suppress("UNCHECKED_CAST")
        return storage as Storage<V>
    }

    companion object {

        fun of(vararg providers: SingleTypeStorageProvider<*, *>) =
            StorageCompoundProvider(providers.asIterable())
    }

    interface SingleTypeStorageProvider<T, V : TransferVariant<T>> {

        val type: ResourceType<T>
        fun getStorage(): Storage<V>
    }

}
