@file:Suppress("UnstableApiUsage")

package dev.adirelle.adicrafter.crafter.impl.recipe.ingredient

import dev.adirelle.adicrafter.crafter.api.recipe.ItemIngredient
import dev.adirelle.adicrafter.crafter.api.storage.ResourceType
import dev.adirelle.adicrafter.crafter.api.storage.StorageProvider
import dev.adirelle.adicrafter.utils.toItemString
import dev.adirelle.adicrafter.utils.withNestedTransaction
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext
import net.minecraft.item.Item

class IngredientWithRemainder(
    private val consumed: ItemIngredient,
    remainder: Item,
) : ItemIngredient by consumed {

    private val remainderVariant = ItemVariant.of(remainder)

    override fun extractFrom(provider: StorageProvider, maxAmount: Long, tx: TransactionContext): Long {
        val fixedAmount = withNestedTransaction(tx) { nested ->
            val (extracted, putBack) = innerExtractFrom(provider, maxAmount, nested)
            if (extracted == putBack) {
                nested.commit()
                return extracted
            }
            nested.abort()
            putBack
        }
        return innerExtractFrom(provider, fixedAmount, tx).second
    }

    private fun innerExtractFrom(
        provider: StorageProvider,
        maxAmount: Long,
        tx: TransactionContext
    ): Pair<Long, Long> {
        val storage = provider.getStorage(ResourceType.ITEM)
        val extracted = consumed.extractFrom(provider, maxAmount, tx)
        val putBack = storage.insert(remainderVariant, extracted, tx)
        return Pair(extracted, putBack)
    }

    override fun toString() = "%s->%s".format(consumed.toString(), remainderVariant.toItemString())
}
