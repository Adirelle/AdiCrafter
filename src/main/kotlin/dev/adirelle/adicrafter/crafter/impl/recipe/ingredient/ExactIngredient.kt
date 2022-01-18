@file:Suppress("UnstableApiUsage")

package dev.adirelle.adicrafter.crafter.impl.recipe.ingredient

import dev.adirelle.adicrafter.crafter.api.recipe.Ingredient
import dev.adirelle.adicrafter.crafter.api.recipe.IngredientFactory
import dev.adirelle.adicrafter.crafter.api.recipe.ItemIngredient
import dev.adirelle.adicrafter.crafter.api.storage.StorageProvider
import dev.adirelle.adicrafter.utils.extensions.toItemString
import dev.adirelle.adicrafter.utils.extensions.toVariant
import net.fabricmc.fabric.api.transfer.v1.storage.TransferVariant
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext
import net.minecraft.item.ItemStack

class ExactIngredient<T : TransferVariant<*>>(
    override val resource: T,
    override val amount: Long
) : Ingredient<T> {

    override fun extractFrom(provider: StorageProvider, maxAmount: Long, tx: TransactionContext) =
        provider.getStorage(resource).extract(resource, maxAmount, tx)

    override fun toString() = "exactly(%d %s)".format(amount, resource.toItemString())

    companion object Factory : IngredientFactory.ItemIngredientFactory {

        override fun create(stack: ItemStack, amount: Long): ItemIngredient {
            val baseIngredient = ExactIngredient(stack.toVariant(), amount)
            return stack.item.recipeRemainder
                ?.let { IngredientWithRemainder(baseIngredient, it) }
                ?: baseIngredient
        }
    }
}
