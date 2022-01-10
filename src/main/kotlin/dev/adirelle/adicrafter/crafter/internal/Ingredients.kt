@file:Suppress("UnstableApiUsage")

package dev.adirelle.adicrafter.crafter.internal

import dev.adirelle.adicrafter.utils.extension.withNestedTransaction
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant
import net.fabricmc.fabric.api.transfer.v1.storage.Storage
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext
import net.minecraft.item.Item
import net.minecraft.recipe.Ingredient
import java.util.function.Predicate
import java.util.function.Supplier

object IngredientMatcher {

    fun exactly(variant: ItemVariant) =
        Predicate<ItemVariant> { variant == it }

    fun byTag(ingredient: Ingredient) =
        Predicate<ItemVariant> { Item.getRawId(it.item) in ingredient.matchingItemIds }
}

fun interface IngredientExtractor {

    fun extract(matcher: Predicate<ItemVariant>, amount: Long, tx: TransactionContext): Long
}

class StandardIngredientExtractor(
    private val storageProvider: Supplier<List<Storage<ItemVariant>>>
) : IngredientExtractor {

    override fun extract(matcher: Predicate<ItemVariant>, amount: Long, tx: TransactionContext): Long =
        Extractor(storageProvider.get()).extract(matcher, amount, tx)

    private class Extractor(val storages: List<Storage<ItemVariant>>) {

        fun extract(matcher: Predicate<ItemVariant>, amount: Long, tx: TransactionContext): Long {
            try {
                withNestedTransaction(tx) { nested ->
                    var extracted = 0L
                    val storageIter = storages.iterator()
                    while (extracted < amount && storageIter.hasNext()) {
                        val input = storageIter.next()
                        if (!input.supportsExtraction()) continue
                        val viewIter = input.iterator(nested)
                        while (extracted < amount && viewIter.hasNext()) {
                            val view = viewIter.next()
                            if (matcher.test(view.resource)) {
                                extracted += extractIt(view, view.resource, amount - extracted, input, nested)
                            }
                        }
                    }
                    nested.commit()
                    return extracted
                }
            } catch (e: NoSpaceForRemainderException) {
                return 0L
            }
        }

        private fun extractIt(
            view: StorageView<ItemVariant>,
            resource: ItemVariant,
            amount: Long,
            input: Storage<ItemVariant>,
            tx: TransactionContext
        ): Long {
            val extracted = view.extract(resource, amount, tx)
            resource.item.recipeRemainder?.let { remainder ->
                val putBack = putBack(ItemVariant.of(remainder), extracted, input, tx)
                if (putBack != extracted) {
                    throw NoSpaceForRemainderException()
                }
            }
            return extracted
        }

        private fun putBack(
            resource: ItemVariant,
            amount: Long,
            prefered: Storage<ItemVariant>,
            tx: TransactionContext
        ): Long {
            var inserted = 0L
            if (prefered.supportsInsertion()) {
                inserted += prefered.insert(resource, amount, tx)
            }
            val storageIter = storages.iterator()
            while (inserted < amount && storageIter.hasNext()) {
                val storage = storageIter.next()
                if (!storage.supportsInsertion()) continue
                inserted += storage.insert(resource, amount - inserted, tx)
            }
            return inserted
        }

    }

    private class NoSpaceForRemainderException : Exception()
}

