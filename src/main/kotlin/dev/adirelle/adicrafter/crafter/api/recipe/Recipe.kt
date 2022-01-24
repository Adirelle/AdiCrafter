@file:Suppress("UnstableApiUsage")

package dev.adirelle.adicrafter.crafter.api.recipe

import dev.adirelle.adicrafter.crafter.api.Crafter
import dev.adirelle.adicrafter.crafter.api.storage.StorageProvider
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant
import net.fabricmc.fabric.api.transfer.v1.storage.base.ResourceAmount
import net.minecraft.item.ItemStack
import net.minecraft.world.World

interface Recipe {

    fun isEmpty(): Boolean = false
    fun createCrafter(provider: StorageProvider, listener: Crafter.Listener): Crafter

    val output: ResourceAmount<ItemVariant>

    companion object EMPTY : Recipe {

        override val output = ResourceAmount(ItemVariant.blank(), 0)

        override fun isEmpty(): Boolean = true
        override fun createCrafter(provider: StorageProvider, listener: Crafter.Listener) = Crafter.EMPTY
    }

    fun interface Factory {

        fun create(world: World?, grid: List<ItemStack>): Recipe
    }

}
