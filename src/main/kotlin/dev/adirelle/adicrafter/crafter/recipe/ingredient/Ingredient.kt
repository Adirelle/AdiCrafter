@file:Suppress("UnstableApiUsage")

package dev.adirelle.adicrafter.crafter.recipe.ingredient

import dev.adirelle.adicrafter.crafter.storage.StorageProvider
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant
import net.fabricmc.fabric.api.transfer.v1.storage.TransferVariant
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext
import net.minecraft.fluid.Fluid
import net.minecraft.item.Item

interface Ingredient<T, V : TransferVariant<T>> {

    val resource: V
    val amount: Long

    fun extractFrom(provider: StorageProvider, maxAmount: Long, tx: TransactionContext): Long

    fun <U : TransferVariant<*>> matches(other: U): Boolean =
        resource.`object` == other.`object`
}

typealias ItemIngredient = Ingredient<Item, ItemVariant>
typealias FluidIngredient = Ingredient<Fluid, FluidVariant>
