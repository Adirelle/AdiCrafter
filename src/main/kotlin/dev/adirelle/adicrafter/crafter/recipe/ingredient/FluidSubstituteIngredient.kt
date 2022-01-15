@file:Suppress("UnstableApiUsage")

package dev.adirelle.adicrafter.crafter.recipe.ingredient

import dev.adirelle.adicrafter.crafter.storage.StorageProvider
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext
import net.minecraft.fluid.Fluid
import net.minecraft.item.Item

class FluidSubstituteIngredient(
    private val fluid: ExactIngredient<Fluid>,
    private val item: Ingredient<Item>
) : Ingredient<Item> {

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
