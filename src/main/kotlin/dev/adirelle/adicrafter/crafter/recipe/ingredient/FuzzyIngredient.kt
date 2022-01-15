@file:Suppress("UnstableApiUsage")

package dev.adirelle.adicrafter.crafter.recipe.ingredient

import dev.adirelle.adicrafter.crafter.storage.StorageProvider
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext

class FuzzyIngredient<T>(
    private val alternatives: Collection<Ingredient<T>>,
    override val amount: Long
) : Ingredient<T> {

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

    override fun toString() = alternatives.joinToString(",", "oneOf[", "]")
}
