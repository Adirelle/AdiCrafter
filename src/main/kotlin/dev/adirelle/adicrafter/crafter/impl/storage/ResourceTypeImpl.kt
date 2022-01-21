@file:Suppress("UnstableApiUsage")

package dev.adirelle.adicrafter.crafter.impl.storage

import dev.adirelle.adicrafter.crafter.api.storage.ResourceType
import net.fabricmc.fabric.api.transfer.v1.storage.TransferVariant

data class ResourceTypeImpl<T : TransferVariant<*>>(
    override val resourceClass: Class<T>,
    override val blank: T
) : ResourceType<T> {

    override fun <U : TransferVariant<*>> isTypeOf(resource: U) =
        resourceClass.isAssignableFrom(resource::class.java)
}
