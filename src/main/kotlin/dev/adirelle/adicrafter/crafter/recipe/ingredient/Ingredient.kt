@file:Suppress("UnstableApiUsage")

package dev.adirelle.adicrafter.crafter.recipe.ingredient

import net.fabricmc.fabric.api.transfer.v1.storage.TransferVariant
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext

interface Ingredient<T : TransferVariant<*>> {

    val amount: Long

    fun extractFrom(provider: StorageProvider, maxAmount: Long, tx: TransactionContext): Long
}
