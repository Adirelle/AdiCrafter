@file:Suppress("UnstableApiUsage")

package dev.adirelle.adicrafter.crafter.recipe.ingredient

import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView
import net.fabricmc.fabric.api.transfer.v1.storage.TransferVariant
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext

open class ExactIngredient<T : TransferVariant<*>>(
    val resource: T,
    override val amount: Long
) : Ingredient<T> {

    override val resourceType: ResourceType<T> = ResourceType.typeOf(resource)

    override fun extractFrom(view: StorageView<T>, maxAmount: Long, tx: TransactionContext) =
        view.extract(resource, maxAmount, tx)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ExactIngredient<*>) return false

        if (amount != other.amount) return false
        if (resource != other.resource) return false

        return true
    }

    override fun hashCode() =
        amount.hashCode() * 31 + resource.hashCode()

    class Factory : IngredientFactory<ItemVariant, ExactIngredient<ItemVariant>, ItemVariant> {

        override fun create(input: ItemVariant, amount: Long): ExactIngredient<ItemVariant> =
            if (input.item.hasRecipeRemainder())
                IngredientWithRemainder(input, ItemVariant.of(input.item.recipeRemainder), amount)
            else
                ExactIngredient(input, amount)

    }
}
