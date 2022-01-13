@file:Suppress("UnstableApiUsage")

package dev.adirelle.adicrafter.crafter.recipe.ingredient

import dev.adirelle.adicrafter.utils.withNestedTransaction
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant
import net.fabricmc.fabric.api.transfer.v1.storage.Storage
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext

class IngredientWithRemainder(
    item: ItemVariant,
    val remainder: ItemVariant,
    amount: Long
) : ExactIngredient<ItemVariant>(item, amount) {

    override fun extractFrom(storage: Storage<ItemVariant>, maxAmount: Long, tx: TransactionContext): Long {
        val fixedAmount = withNestedTransaction(tx) { nested ->
            val (extracted, putBack) = innerExtractFrom(storage, maxAmount, nested)
            if (extracted == putBack) {
                nested.commit()
                return extracted
            }
            nested.abort()
            putBack
        }
        return innerExtractFrom(storage, fixedAmount, tx).second
    }

    private fun innerExtractFrom(
        storage: Storage<ItemVariant>,
        maxAmount: Long,
        tx: TransactionContext
    ): Pair<Long, Long> {
        val extracted = super.extractFrom(storage, maxAmount, tx)
        val putBack = storage.insert(remainder, extracted, tx)
        return Pair(extracted, putBack)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is IngredientWithRemainder) return false
        if (!super.equals(other)) return false

        if (remainder != other.remainder) return false

        return true
    }

    override fun hashCode() =
        super.hashCode() * 31 + remainder.hashCode()
}
