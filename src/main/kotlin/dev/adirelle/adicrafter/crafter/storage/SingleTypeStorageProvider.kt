package dev.adirelle.adicrafter.crafter.storage

interface SingleTypeStorageProvider<T> : StorageProvider {

    val type: ResourceType<T>
}

