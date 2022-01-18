@file:Suppress("UnstableApiUsage")

package dev.adirelle.adicrafter.crafter.api.storage

import net.fabricmc.fabric.api.transfer.v1.storage.Storage
import net.fabricmc.fabric.api.transfer.v1.storage.TransferVariant

fun interface SingleTypeStorageProvider<T : TransferVariant<*>> {

    fun getStorage(): Storage<T>

    companion object {

        fun <T : TransferVariant<*>> of(storage: Storage<T>): SingleTypeStorageProvider<T> =
            SingleTypeStorageProvider { storage }
    }
}
