package dev.adirelle.adicrafter.crafter.api.recipe

import dev.adirelle.adicrafter.crafter.api.Crafter
import dev.adirelle.adicrafter.crafter.api.storage.StorageProvider
import net.minecraft.item.ItemStack
import net.minecraft.world.World

interface Recipe {

    fun isEmpty(): Boolean = false
    fun createCrafter(provider: StorageProvider): Crafter

    companion object EMPTY : Recipe {

        override fun isEmpty(): Boolean = true
        override fun createCrafter(provider: StorageProvider) = Crafter.EMPTY
    }

    fun interface Factory {

        fun create(world: World?, grid: List<ItemStack>): Recipe
    }
    
}
