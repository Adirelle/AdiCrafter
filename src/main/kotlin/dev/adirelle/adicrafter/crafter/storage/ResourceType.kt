@file:Suppress("UnstableApiUsage")

package dev.adirelle.adicrafter.crafter.storage

import net.fabricmc.fabric.api.transfer.v1.storage.TransferVariant
import net.minecraft.fluid.Fluid
import net.minecraft.item.Item

class ResourceType<T>(
    val klass: Class<T>
) {

    override fun equals(other: Any?) =
        other === this || (other is ResourceType<*> && other.klass == klass)

    override fun hashCode() =
        klass.hashCode()

    override fun toString(): String = klass.simpleName

    companion object {

        val ITEM = ResourceType(Item::class.java)
        val FLUID = ResourceType(Fluid::class.java)

        fun <T> of(obj: T): ResourceType<T> {
            val type = when (obj) {
                is Item  -> ITEM
                is Fluid -> FLUID
                else     -> throw IllegalArgumentException("unsupported resource: $obj")
            }
            @Suppress("UNCHECKED_CAST")
            return type as ResourceType<T>
        }

        fun <T> of(resource: TransferVariant<T>): ResourceType<T> =
            of(resource.getObject()!!)
    }
}
