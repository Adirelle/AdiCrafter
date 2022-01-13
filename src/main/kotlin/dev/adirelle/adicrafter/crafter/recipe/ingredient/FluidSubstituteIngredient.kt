@file:Suppress("UnstableApiUsage")

package dev.adirelle.adicrafter.crafter.recipe.ingredient

import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext

class FluidSubstituteIngredient(
    private val fluid: ExactIngredient<FluidVariant>,
    private val item: Ingredient<ItemVariant>
) : Ingredient<ItemVariant> {

    override val resourceType by item::resourceType
    override val amount by item::amount

    override fun extractFrom(view: StorageView<ItemVariant>, maxAmount: Long, tx: TransactionContext) =
        item.extractFrom(view, maxAmount, tx)

    override fun extractFrom(provider: StorageProvider, maxAmount: Long, tx: TransactionContext): Long {
        var extracted = fluid.extractFrom(provider, maxAmount * fluid.amount, tx) / fluid.amount
        if (extracted < maxAmount) {
            extracted += item.extractFrom(provider, maxAmount - extracted, tx)
        }
        return extracted
    }

    override fun toString() = "{%s/%s}".format(fluid, item)
}
