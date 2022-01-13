@file:Suppress("UnstableApiUsage")

package dev.adirelle.adicrafter.crafter.recipe.ingredient

import dev.adirelle.adicrafter.utils.toItemString
import dev.adirelle.adicrafter.utils.withNestedTransaction
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant
import net.fabricmc.fabric.api.transfer.v1.storage.Storage
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext

class IngredientWithRemainder(
    private val consumed: Ingredient<ItemVariant>,
    val remainder: ItemVariant,
) : Ingredient<ItemVariant> {

    override val resourceType = ResourceType.ITEM
    override val amount by consumed::amount

    override fun extractFrom(view: StorageView<ItemVariant>, maxAmount: Long, tx: TransactionContext) =
        consumed.extractFrom(view, maxAmount, tx)

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
        val extracted = consumed.extractFrom(storage, maxAmount, tx)
        val putBack = storage.insert(remainder, extracted, tx)
        return Pair(extracted, putBack)
    }

    override fun toString() = "%s->%s".format(consumed.toString(), remainder.toItemString())

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is IngredientWithRemainder) return false

        if (consumed != other.consumed) return false
        if (remainder != other.remainder) return false
        if (resourceType != other.resourceType) return false

        return true
    }

    override fun hashCode(): Int {
        var result = consumed.hashCode()
        result = 31 * result + remainder.hashCode()
        result = 31 * result + resourceType.hashCode()
        return result
    }
}
