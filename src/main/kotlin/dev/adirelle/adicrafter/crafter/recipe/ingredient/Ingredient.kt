@file:Suppress("UnstableApiUsage")

package dev.adirelle.adicrafter.crafter.recipe.ingredient

import dev.adirelle.adicrafter.crafter.storage.StorageProvider
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext

interface Ingredient<T> {

    val amount: Long

    fun extractFrom(provider: StorageProvider, maxAmount: Long, tx: TransactionContext): Long
}
