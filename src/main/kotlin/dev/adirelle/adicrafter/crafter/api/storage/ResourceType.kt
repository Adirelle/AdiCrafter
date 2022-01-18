@file:Suppress("UnstableApiUsage")

package dev.adirelle.adicrafter.crafter.api.storage

import dev.adirelle.adicrafter.crafter.api.power.PowerVariant
import dev.adirelle.adicrafter.crafter.impl.storage.ResourceTypeImpl
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant
import net.fabricmc.fabric.api.transfer.v1.storage.TransferVariant

interface ResourceType<T : TransferVariant<*>> {

    val resourceClass: Class<T>
    val blank: T

    fun <U : TransferVariant<*>> isTypeOf(resource: U): Boolean

    companion object {

        val ITEM = ResourceTypeImpl(ItemVariant::class.java, ItemVariant.blank())
        val FLUID = ResourceTypeImpl(FluidVariant::class.java, FluidVariant.blank())
        val POWER = ResourceTypeImpl(PowerVariant::class.java, PowerVariant.INSTANCE)

        fun <T : TransferVariant<*>> of(resource: T): ResourceType<T> {
            val type: ResourceType<*> = when {
                ITEM.isTypeOf(resource)  -> ITEM
                FLUID.isTypeOf(resource) -> FLUID
                POWER.isTypeOf(resource) -> POWER
                else                     -> throw IllegalArgumentException("unsupported resource: $resource")
            }
            @Suppress("UNCHECKED_CAST")
            return type as ResourceType<T>
        }

        inline fun <reified T : TransferVariant<*>> of(): ResourceType<T> {
            val type: ResourceType<*> = when {
                ITEM.resourceClass.isAssignableFrom(T::class.java)  -> ITEM
                FLUID.resourceClass.isAssignableFrom(T::class.java) -> FLUID
                POWER.resourceClass.isAssignableFrom(T::class.java) -> POWER
                else                                                -> throw IllegalArgumentException("unsupported resource class")
            }
            @Suppress("UNCHECKED_CAST")
            return type as ResourceType<T>
        }
    }

}
