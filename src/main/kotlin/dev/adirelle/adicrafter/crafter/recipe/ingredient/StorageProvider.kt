@file:Suppress("UnstableApiUsage")

package dev.adirelle.adicrafter.crafter.recipe.ingredient

import net.fabricmc.fabric.api.transfer.v1.storage.Storage
import net.fabricmc.fabric.api.transfer.v1.storage.TransferVariant

interface StorageProvider {

    fun <T : TransferVariant<*>> getStorage(resourceType: ResourceType<T>): Storage<T>
}
