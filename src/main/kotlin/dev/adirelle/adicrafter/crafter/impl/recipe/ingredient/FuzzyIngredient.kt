@file:Suppress("UnstableApiUsage")

package dev.adirelle.adicrafter.crafter.impl.recipe.ingredient

import dev.adirelle.adicrafter.crafter.api.recipe.Ingredient
import dev.adirelle.adicrafter.crafter.api.storage.StorageProvider
import net.fabricmc.fabric.api.transfer.v1.storage.TransferVariant
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext

class FuzzyIngredient<T : TransferVariant<*>>(
    private val alternatives: Collection<Ingredient<T>>,
    override val amount: Long
) : Ingredient<T> {

    override val resource = alternatives.first().resource

    override fun extractFrom(provider: StorageProvider, maxAmount: Long, tx: TransactionContext): Long {
        var extracted = 0L
        for (alternative in alternatives) {
            extracted += alternative.extractFrom(provider, maxAmount - extracted, tx)
            if (extracted >= maxAmount) {
                break
            }
        }
        return extracted
    }

    override fun <Z : TransferVariant<*>> matches(other: Z) =
        alternatives.any { it.matches(other) }

    override fun toString() = alternatives.joinToString(",", "oneOf[", "]")
}
