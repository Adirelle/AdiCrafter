@file:Suppress("UnstableApiUsage")

package dev.adirelle.adicrafter.crafter.recipe.ingredient

import dev.adirelle.adicrafter.utils.extensions.toItemString
import net.fabricmc.fabric.api.transfer.v1.storage.TransferVariant
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext

class ExactIngredient<T : TransferVariant<*>>(
    val resource: T,
    override val amount: Long
) : Ingredient<T> {

    val resourceType: ResourceType<T> = ResourceType.typeOf(resource)

    override fun extractFrom(provider: StorageProvider, maxAmount: Long, tx: TransactionContext) =
        provider.getStorage(resourceType).extract(resource, maxAmount, tx)

    override fun toString() = "exactly(%d %s)".format(amount, resource.toItemString())
}
