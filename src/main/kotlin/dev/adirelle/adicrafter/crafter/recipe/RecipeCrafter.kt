@file:Suppress("UnstableApiUsage")

package dev.adirelle.adicrafter.crafter

import dev.adirelle.adicrafter.utils.withNestedTransaction
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant
import net.fabricmc.fabric.api.transfer.v1.storage.Storage
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext
import kotlin.math.min

class RecipeCrafter(
    private val recipe: Recipe,
    private val inputs: Storage<ItemVariant>,
) : StorageView<ItemVariant> {

    private val outputResource: ItemVariant = ItemVariant.of(recipe.output)
    private val outputAmount: Long = recipe.output.count.toLong()
    private val outputCapacity: Long = outputResource.item.maxCount.toLong()

    override fun isResourceBlank() = outputResource.isBlank
    override fun getResource() = outputResource
    override fun getAmount() = outputAmount
    override fun getCapacity() = outputCapacity

    override fun extract(resource: ItemVariant, maxAmount: Long, tx: TransactionContext): Long {
        if (recipe.isEmpty) return 0L

        val available = withNestedTransaction(tx) { nested ->
            val crafted = craftInternal(maxAmount, nested)
            if (crafted >= maxAmount) {
                nested.commit()
                return crafted
            }
            nested.abort()
            crafted
        }
        return craftInternal(available, tx)
    }

    private fun craftInternal(
        maxAmount: Long,
        tx: TransactionContext
    ): Long {
        val maxBatchs = (maxAmount + outputAmount - 1) / outputAmount
        var craftedBatchs = maxBatchs
        for (ingredient in recipe.ingredients) {
            val extracted = ingredient.extractFrom(inputs, ingredient.amount * maxBatchs, tx)
            craftedBatchs = min(craftedBatchs, extracted / ingredient.amount)
        }
        return outputAmount * craftedBatchs
    }
}
