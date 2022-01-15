@file:Suppress("UnstableApiUsage")

package dev.adirelle.adicrafter.crafter.recipe.ingredient

import dev.adirelle.adicrafter.crafter.storage.ResourceType
import dev.adirelle.adicrafter.crafter.storage.StorageProvider
import dev.adirelle.adicrafter.utils.toItemString
import dev.adirelle.adicrafter.utils.withNestedTransaction
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext
import net.minecraft.item.Item

class IngredientWithRemainder(
    private val consumed: ItemIngredient,
    val remainder: Item,
) : ItemIngredient {

    override val resource by consumed::resource
    override val amount by consumed::amount

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

    override fun toString() = "%s->%s".format(consumed.toString(), remainder.toItemString())
}
