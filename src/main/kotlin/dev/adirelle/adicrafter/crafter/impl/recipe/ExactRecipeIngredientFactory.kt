package dev.adirelle.adicrafter.crafter.impl.recipe

import dev.adirelle.adicrafter.crafter.api.recipe.Ingredient
import dev.adirelle.adicrafter.crafter.api.recipe.IngredientFactory
import dev.adirelle.adicrafter.crafter.api.recipe.IngredientFactory.ItemIngredientFactory
import net.minecraft.item.ItemStack
import net.minecraft.recipe.CraftingRecipe

class ExactRecipeIngredientFactory(private val itemIngredientFactory: ItemIngredientFactory) : IngredientFactory {

    override fun create(recipe: CraftingRecipe, grid: List<ItemStack>): Collection<Ingredient<*>> =
        grid
            .filterNot { it.isEmpty }
            .groupBy { it.item }
            .map { (_, stacks) -> itemIngredientFactory.create(stacks[0], stacks.size.toLong()) }
}
