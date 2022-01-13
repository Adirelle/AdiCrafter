@file:Suppress("UnstableApiUsage")

package dev.adirelle.adicrafter.crafter.recipe.ingredient

import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView
import net.fabricmc.fabric.api.transfer.v1.storage.TransferVariant
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext
import net.minecraft.recipe.Ingredient as MinecraftIngredient

class FuzzyIngredient<T : TransferVariant<*>>(alternatives: Collection<ExactIngredient<T>>) : Ingredient<T> {

    override val resourceType = alternatives.first().resourceType
    override val amount = alternatives.first().amount

    private val ingredientMap = buildMap { alternatives.forEach { put(it.resource, it) } }

    override fun extractFrom(view: StorageView<T>, maxAmount: Long, tx: TransactionContext): Long =
        ingredientMap.get(view.resource)?.extractFrom(view, maxAmount, tx) ?: 0

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is FuzzyIngredient<*>) return false

        if (amount != other.amount) return false
        if (ingredientMap != other.ingredientMap) return false

        return true
    }

    override fun hashCode() =
        amount.hashCode() * 31 + ingredientMap.hashCode()

    class Factory(private val backing: IngredientFactory<ItemVariant, ExactIngredient<ItemVariant>, ItemVariant>) :
        IngredientFactory<MinecraftIngredient, FuzzyIngredient<ItemVariant>, ItemVariant> {

        override fun create(input: MinecraftIngredient, amount: Long) =
            FuzzyIngredient(input.matchingStacks.map { backing.create(ItemVariant.of(it), amount) })
    }
}
