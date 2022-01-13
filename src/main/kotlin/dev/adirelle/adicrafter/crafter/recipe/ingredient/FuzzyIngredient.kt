@file:Suppress("UnstableApiUsage")

package dev.adirelle.adicrafter.crafter.recipe.ingredient

import net.fabricmc.fabric.api.transfer.v1.storage.StorageView
import net.fabricmc.fabric.api.transfer.v1.storage.TransferVariant
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext

class FuzzyIngredient<T : TransferVariant<*>>(private val alternatives: Collection<Ingredient<T>>) : Ingredient<T> {

    override val resourceType = alternatives.first().resourceType
    override val amount = alternatives.first().amount

    override fun extractFrom(view: StorageView<T>, maxAmount: Long, tx: TransactionContext): Long =
        alternatives
            .map { it.extractFrom(view, maxAmount, tx) }
            .firstOrNull()
            ?: 0L

    override fun toString() = alternatives.joinToString(",", "oneOf[", "]")
}
