@file:Suppress("UnstableApiUsage")

package dev.adirelle.adicrafter.crafter

import dev.adirelle.adicrafter.crafter.internal.IngredientExtractor
import dev.adirelle.adicrafter.utils.withNestedTransaction
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext
import java.util.function.Supplier
import kotlin.math.min

class RecipeCrafter(
    private val recipeProvider: Supplier<Recipe>,
    private val extractor: IngredientExtractor
) : StorageView<ItemVariant> {

    val recipe
        get() = recipeProvider.get()

    override fun extract(resource: ItemVariant, maxAmount: Long, tx: TransactionContext): Long {
        if (recipe.isEmpty) return 0L

        val available = withNestedTransaction(tx) { nested ->
            val crafted = craftInternal(recipe, extractor, maxAmount, nested)
            if (crafted >= maxAmount) {
                nested.commit()
                return crafted
            }
            nested.abort()
            crafted
        }
        return craftInternal(recipe, extractor, available, tx)
    }

    private fun craftInternal(
        recipe: Recipe,
        extractor: IngredientExtractor,
        maxAmount: Long,
        tx: TransactionContext
    ): Long {
        val batchSize = recipe.output.amount
        val batchCount = (maxAmount + batchSize - 1) / batchSize
        var availableBatchs = batchCount
        for (ingredient in recipe.ingredients) {
            val required = ingredient.amount * batchCount
            val found = extractor.extract(ingredient.matcher, required, tx)
            availableBatchs = min(availableBatchs, found / ingredient.amount)
        }
        return batchSize * availableBatchs
    }

    override fun isResourceBlank() = recipe.isEmpty
    override fun getResource() = recipe.output.resource
    override fun getAmount() = recipe.output.amount
    override fun getCapacity() = resource.item.maxCount.toLong()
}
