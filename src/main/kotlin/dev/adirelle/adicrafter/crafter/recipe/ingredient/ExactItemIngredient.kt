@file:Suppress("UnstableApiUsage")

package dev.adirelle.adicrafter.crafter.recipe.ingredient

import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext
import net.minecraft.item.Item

open class ExactItemIngredient(
    item: Item,
    override val amount: Long
) : Ingredient<ItemVariant> {

    val resource: ItemVariant = ItemVariant.of(item)

    override fun extractFrom(view: StorageView<ItemVariant>, maxAmount: Long, tx: TransactionContext) =
        view.extract(resource, maxAmount, tx)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ExactItemIngredient) return false

        if (amount != other.amount) return false
        if (resource != other.resource) return false

        return true
    }

    override fun hashCode() =
        amount.hashCode() * 31 + resource.hashCode()
}
