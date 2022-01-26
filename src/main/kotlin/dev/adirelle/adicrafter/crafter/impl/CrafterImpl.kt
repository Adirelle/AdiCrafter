@file:Suppress("UnstableApiUsage")

package dev.adirelle.adicrafter.crafter.impl

import dev.adirelle.adicrafter.crafter.api.Crafter
import dev.adirelle.adicrafter.crafter.api.recipe.Ingredient
import dev.adirelle.adicrafter.crafter.api.recipe.ItemIngredient
import dev.adirelle.adicrafter.crafter.api.storage.StorageProvider
import dev.adirelle.adicrafter.utils.withNestedTransaction
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext.OuterCloseCallback
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext.Result
import net.minecraft.item.ItemConvertible
import java.util.*
import kotlin.math.min

class CrafterImpl(
    private val resource: ItemVariant,
    private val amount: Long,
    private val ingredients: Iterable<Ingredient<*>>,
    private val storageProvider: StorageProvider,
    private val listener: Crafter.Listener
) : Crafter, OuterCloseCallback {

    override fun isResourceBlank() = resource.isBlank
    override fun getResource() = resource
    override fun getAmount() = amount
    override fun getCapacity() = resource.item.maxCount.toLong()

    override fun findIngredientFor(item: ItemConvertible): Optional<ItemIngredient> {
        val realItem = item.asItem()
        @Suppress("UNCHECKED_CAST")
        return Optional.ofNullable(
            ingredients.firstOrNull { it.resource.`object` == realItem }
        ) as Optional<ItemIngredient>
    }

    override fun extract(resource: ItemVariant, maxAmount: Long, tx: TransactionContext): Long {
        if (resource.isBlank || maxAmount < 1) return 0L
        val available = withNestedTransaction(tx) { nested ->
            val (exact, crafted) = craftInternal(maxAmount, nested)
            if (exact) {
                nested.commit()
                return crafted
            }
            nested.abort()
            crafted
        }
        return if (available > 0)
            craftInternal(available, tx).second
        else
            0L
    }

    override fun simulateExtract(resource: ItemVariant, maxAmount: Long, tx: TransactionContext?): Long {
        if (resource.isBlank || maxAmount < 1) return 0L
        withNestedTransaction(tx) { nested ->
            val result = craftInternal(maxAmount, nested)
            nested.abort()
            return result.second
        }
    }

    private fun craftInternal(
        maxAmount: Long,
        tx: TransactionContext
    ): Pair<Boolean, Long> {
        val maxBatchs = (maxAmount + amount - 1) / amount
        var craftedBatchs = maxBatchs
        for (ingredient in ingredients) {
            val extracted = ingredient.extractFrom(storageProvider, ingredient.amount * maxBatchs, tx)
            craftedBatchs = min(craftedBatchs, extracted / ingredient.amount)
            if (craftedBatchs == 0L) break
        }
        if (craftedBatchs > 0) {
            tx.addOuterCloseCallback(this)
        }
        return Pair(craftedBatchs == maxBatchs, amount * craftedBatchs)
    }

    override fun afterOuterClose(result: Result) {
        if (result.wasCommitted()) {
            listener.onCrafterUpdate()
        }
    }
}
