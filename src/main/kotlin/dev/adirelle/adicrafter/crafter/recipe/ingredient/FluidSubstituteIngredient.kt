@file:Suppress("UnstableApiUsage")

package dev.adirelle.adicrafter.crafter.recipe.ingredient

import dev.adirelle.adicrafter.crafter.storage.StorageProvider
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext

class FluidSubstituteIngredient(
    private val fluid: FluidIngredient,
    private val item: ItemIngredient
) : ItemIngredient {

    override val resource by item::resource
    override val amount by item::amount

    override fun extractFrom(provider: StorageProvider, maxAmount: Long, tx: TransactionContext): Long {
        var extracted = fluid.extractFrom(provider, maxAmount * fluid.amount, tx) / fluid.amount
        if (extracted < maxAmount) {
            extracted += item.extractFrom(provider, maxAmount - extracted, tx)
        }
        return extracted
    }

    override fun toString() = "{%s/%s}".format(fluid, item)
}
