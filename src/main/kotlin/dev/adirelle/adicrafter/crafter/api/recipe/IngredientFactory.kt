@file:Suppress("UnstableApiUsage")

package dev.adirelle.adicrafter.crafter.api.recipe

import net.minecraft.item.ItemStack
import net.minecraft.recipe.CraftingRecipe

fun interface IngredientFactory {

    fun create(recipe: CraftingRecipe, grid: List<ItemStack>): Collection<Ingredient<*>>

    fun interface ItemIngredientFactory {

        fun create(stack: ItemStack, amount: Long): ItemIngredient
    }
}
