@file:Suppress("UnstableApiUsage")

package dev.adirelle.adicrafter.crafter.recipe.ingredient

import net.fabricmc.fabric.api.lookup.v1.block.BlockApiLookup
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant
import net.fabricmc.fabric.api.transfer.v1.storage.Storage
import net.fabricmc.fabric.api.transfer.v1.storage.TransferVariant
import net.minecraft.util.math.Direction

data class ResourceType<T : TransferVariant<*>>(
    val resourceClass: Class<T>,
    val storageLookup: BlockApiLookup<Storage<T>, Direction>
) {

    companion object {

        val ITEM = ResourceType(ItemVariant::class.java, ItemStorage.SIDED)
        val FLUID = ResourceType(FluidVariant::class.java, FluidStorage.SIDED)

        @Suppress("UNCHECKED_CAST")
        fun <T : TransferVariant<*>> typeOf(resource: T): ResourceType<T> =
            when (resource) {
                is ItemVariant  -> ITEM
                is FluidVariant -> FLUID
                else            -> throw RuntimeException("unsupported variant type")
            } as ResourceType<T>
    }
}
