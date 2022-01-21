@file:Suppress("UnstableApiUsage")

package dev.adirelle.adicrafter.crafter.api.recipe

import dev.adirelle.adicrafter.crafter.api.power.PowerVariant
import dev.adirelle.adicrafter.crafter.api.storage.StorageProvider
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant
import net.fabricmc.fabric.api.transfer.v1.storage.TransferVariant
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext

interface Ingredient<T : TransferVariant<*>> {

    val resource: T
    val amount: Long

    fun extractFrom(provider: StorageProvider, maxAmount: Long, tx: TransactionContext): Long

    fun <U : TransferVariant<*>> matches(other: U): Boolean =
        resource.`object` == other.`object`
}

typealias FluidIngredient = Ingredient<FluidVariant>
typealias ItemIngredient = Ingredient<ItemVariant>
typealias PowerIngredient = Ingredient<PowerVariant>
