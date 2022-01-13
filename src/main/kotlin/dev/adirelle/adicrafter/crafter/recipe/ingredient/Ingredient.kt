@file:Suppress("UnstableApiUsage")

package dev.adirelle.adicrafter.crafter.recipe.ingredient

import dev.adirelle.adicrafter.utils.extensions.takeWhile
import net.fabricmc.fabric.api.transfer.v1.storage.Storage
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView
import net.fabricmc.fabric.api.transfer.v1.storage.TransferVariant
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext

interface Ingredient<T : TransferVariant<*>> {

    val resourceType: ResourceType<T>
    val amount: Long

    fun extractFrom(view: StorageView<T>, maxAmount: Long, tx: TransactionContext): Long

    fun extractFrom(storage: Storage<T>, maxAmount: Long, tx: TransactionContext): Long {
        var extracted = 0L
        storage.iterator(tx)
            .takeWhile { extracted < maxAmount }
            .forEach { view ->
                extracted += extractFrom(view, maxAmount - extracted, tx)
            }
        return extracted
    }

    fun extractFrom(provider: StorageProvider, maxAmount: Long, tx: TransactionContext) =
        extractFrom(provider.getStorage(resourceType), maxAmount, tx)
}
