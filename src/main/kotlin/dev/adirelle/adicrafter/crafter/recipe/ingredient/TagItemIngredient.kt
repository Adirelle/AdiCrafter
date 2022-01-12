@file:Suppress("UnstableApiUsage")

package dev.adirelle.adicrafter.crafter.recipe.ingredient

import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext
import net.minecraft.item.ItemConvertible

class TagItemIngredient(
    items: Collection<ItemConvertible>,
    override val amount: Long
) : Ingredient<ItemVariant> {

    private val ingredientMap: Map<ItemVariant, Ingredient<ItemVariant>> = buildMap {
        items.forEach { item ->
            put(ItemVariant.of(item), Ingredient.exactly(item, amount.toInt()))
        }
    }

    override fun extractFrom(view: StorageView<ItemVariant>, maxAmount: Long, tx: TransactionContext): Long =
        ingredientMap.get(view.resource)?.extractFrom(view, maxAmount, tx) ?: 0

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TagItemIngredient) return false

        if (amount != other.amount) return false
        if (ingredientMap != other.ingredientMap) return false

        return true
    }

    override fun hashCode() =
        amount.hashCode() * 31 + ingredientMap.hashCode()

}
