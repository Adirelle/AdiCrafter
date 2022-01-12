@file:Suppress("UnstableApiUsage")

package dev.adirelle.adicrafter.crafter.recipe.ingredient

import dev.adirelle.adicrafter.utils.extensions.takeWhile
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant
import net.fabricmc.fabric.api.transfer.v1.storage.Storage
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView
import net.fabricmc.fabric.api.transfer.v1.storage.TransferVariant
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext
import net.minecraft.item.ItemConvertible
import net.minecraft.item.ItemStack

interface Ingredient<T : TransferVariant<*>> {

    val amount: Long

    fun extractFrom(view: StorageView<T>, maxAmount: Long, tx: TransactionContext): Long

    fun extractFrom(storage: Storage<T>, maxAmount: Long, tx: TransactionContext): Long {
        var extracted = 0L
        storage.iterator(tx)
            .takeWhile { extracted < maxAmount }
            .forEach { view ->
                extracted += extractFrom(view, maxAmount - extracted, tx)
            }
        return extracted
    }

    companion object {

        fun exactly(itemConvertible: ItemConvertible, amount: Int): Ingredient<ItemVariant> =
            itemConvertible.asItem().let { item ->
                item.recipeRemainder?.let { remainder ->
                    IngredientWithRemainder(item, remainder, amount.toLong())
                } ?: ExactItemIngredient(item, amount.toLong())
            }

        fun anyOf(stacks: Array<ItemStack>, amount: Int): Ingredient<ItemVariant> =
            TagItemIngredient(stacks.map { it.item }, amount.toLong())

    }

}
