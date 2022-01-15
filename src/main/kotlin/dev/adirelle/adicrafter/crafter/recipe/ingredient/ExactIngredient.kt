@file:Suppress("UnstableApiUsage")

package dev.adirelle.adicrafter.crafter.recipe.ingredient

import dev.adirelle.adicrafter.crafter.storage.StorageProvider
import dev.adirelle.adicrafter.utils.extensions.toItemString
import net.fabricmc.fabric.api.transfer.v1.storage.TransferVariant
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext

class ExactIngredient<T, V : TransferVariant<T>>(
    override val resource: V,
    override val amount: Long
) : Ingredient<T, V> {

    override fun extractFrom(provider: StorageProvider, maxAmount: Long, tx: TransactionContext) =
        provider.getStorage(resource).extract(resource, maxAmount, tx)

    override fun toString() = "exactly(%d %s)".format(amount, resource.toItemString())
}
