@file:Suppress("UnstableApiUsage")

package dev.adirelle.adicrafter.crafter

import dev.adirelle.adicrafter.crafter.internal.InputProvider
import dev.adirelle.adicrafter.utils.extension.withNestedTransaction
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
            var found = extract(inputs, ingredient.resource, required, tx)
            ingredient.resource.item.recipeRemainder?.let { remainder ->
                found = min(found, putBack(inputs, ItemVariant.of(remainder), found, tx))
            }
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
        var extracted = 0L
        inputs@ for (input in inputs) {
            if (!input.supportsExtraction()) continue
            for (view in input.iterator(tx)) {
                extracted += view.extract(resource, maxAmount - extracted, tx)
                if (extracted >= maxAmount) break@inputs
            }
        }
        return extracted
    }

    private fun putBack(
        inputs: List<Storage<ItemVariant>>,
        resource: ItemVariant?,
        amount: Long,
        tx: TransactionContext
    ): Long {
        var inserted = 0L
        for (input in inputs) {
            if (!input.supportsInsertion()) continue
            inserted += input.insert(resource, amount - inserted, tx)
            if (inserted >= amount) break
        }
        return inserted
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
