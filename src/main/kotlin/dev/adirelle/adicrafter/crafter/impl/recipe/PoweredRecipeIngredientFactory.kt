package dev.adirelle.adicrafter.crafter.impl.recipe

import dev.adirelle.adicrafter.crafter.api.power.PowerVariant
import dev.adirelle.adicrafter.crafter.api.recipe.Ingredient
import dev.adirelle.adicrafter.crafter.api.recipe.IngredientFactory
import dev.adirelle.adicrafter.crafter.impl.recipe.ingredient.ExactIngredient
import net.minecraft.item.ItemStack
import net.minecraft.recipe.CraftingRecipe

class PoweredRecipeIngredientFactory(
    private val backing: IngredientFactory,
    cost: Long
) : IngredientFactory {

    private val powerIngredient = ExactIngredient(PowerVariant.INSTANCE, cost)

    override fun create(recipe: CraftingRecipe, grid: List<ItemStack>): Collection<Ingredient<*>> =
        buildList {
            addAll(backing.create(recipe, grid))
            add(powerIngredient)
        }
}
