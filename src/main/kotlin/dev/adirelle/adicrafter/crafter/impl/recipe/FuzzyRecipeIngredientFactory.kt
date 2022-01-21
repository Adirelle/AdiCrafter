package dev.adirelle.adicrafter.crafter.impl.recipe

import dev.adirelle.adicrafter.crafter.api.recipe.Ingredient
import dev.adirelle.adicrafter.crafter.api.recipe.IngredientFactory
import dev.adirelle.adicrafter.crafter.api.recipe.IngredientFactory.ItemIngredientFactory
import dev.adirelle.adicrafter.crafter.impl.recipe.ingredient.FuzzyIngredient
import net.minecraft.item.ItemStack
import net.minecraft.recipe.CraftingRecipe

class FuzzyRecipeIngredientFactory(private val itemIngredientFactory: ItemIngredientFactory) : IngredientFactory {

    override fun create(recipe: CraftingRecipe, grid: List<ItemStack>): Collection<Ingredient<*>> =
        recipe.ingredients
            .filterNot { it.isEmpty }
            .groupBy { it }
            .map { (ingredient, list) -> mapIngredient(ingredient.matchingStacks, list.size.toLong()) }

    private fun mapIngredient(alternatives: Array<ItemStack>, amount: Long): Ingredient<*> =
        if (alternatives.size == 1)
            itemIngredientFactory.create(alternatives[0], amount)
        else
            FuzzyIngredient(alternatives.map { itemIngredientFactory.create(it, 1) }, amount)
}
