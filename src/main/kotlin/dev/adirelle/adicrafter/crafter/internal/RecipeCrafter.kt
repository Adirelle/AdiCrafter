@file:Suppress("UnstableApiUsage")

package dev.adirelle.adicrafter.crafter

import dev.adirelle.adicrafter.crafter.internal.InputProvider
import dev.adirelle.adicrafter.utils.extension.withNestedTransaction
import dev.adirelle.adicrafter.utils.extension.withOuterTransaction
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant
import net.fabricmc.fabric.api.transfer.v1.storage.Storage
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext
import java.util.function.Supplier
import kotlin.math.min

class RecipeCrafter(
    private val recipeProvider: Supplier<OptionalRecipe>,
    private val inputProvider: InputProvider
) {

    val recipe: OptionalRecipe
        get() = recipeProvider.get()

    fun craft(maxAmount: Long, tx: TransactionContext): Long =
        ifNotEmpty { recipe, inputs ->
            val available = withNestedTransaction(tx) { tx ->
                val crafted = craftInternal(recipe, inputs, maxAmount, tx)
                if (crafted >= maxAmount) {
                    tx.commit()
                    return crafted
                }
                tx.abort()
                crafted
            }
            craftInternal(recipe, inputs, available, tx)
        }

    fun simulateCraft(maxAmount: Long): Long =
        ifNotEmpty { recipe, inputs ->
            withOuterTransaction { tx ->
                craftInternal(recipe, inputs, maxAmount, tx).also {
                    tx.abort()
                }
            }
        }

    private fun craftInternal(
        recipe: OptionalRecipe,
        inputs: List<Storage<ItemVariant>>,
        maxAmount: Long,
        tx: TransactionContext
    ): Long {
        val batchSize = recipe.output.amount
        val batchCount = (maxAmount + batchSize - 1) / batchSize
        var availableBatchs = batchCount
        for (ingredient in recipe.ingredients) {
            val required = ingredient.amount * batchCount
            val found = extract(inputs, ingredient.resource, required, tx)
            availableBatchs = min(availableBatchs, found / ingredient.amount)
        }
        return batchSize * availableBatchs
    }

    private fun extract(
        inputs: List<Storage<ItemVariant>>,
        resource: ItemVariant,
        maxAmount: Long,
        tx: TransactionContext
    ): Long {
        val inputIterator = inputs.iterator()
        var extracted = 0L
        while (extracted < maxAmount && inputIterator.hasNext()) {
            val viewIterator = inputIterator.next().iterator(tx)
            while (extracted < maxAmount && viewIterator.hasNext()) {
                extracted += viewIterator.next().extract(resource, maxAmount - extracted, tx)
            }
        }
        return extracted
    }

    private inline fun ifNotEmpty(
        block: (recipe: OptionalRecipe, inputs: List<Storage<ItemVariant>>) -> Long
    ): Long =
        inputProvider.get()?.let { inputs ->
            recipeProvider.get().takeUnless { it.isEmpty }?.let { recipe ->
                block(recipe, inputs)
            }
        } ?: 0L
}
