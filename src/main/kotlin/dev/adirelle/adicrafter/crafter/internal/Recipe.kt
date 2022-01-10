@file:Suppress("UnstableApiUsage")

package dev.adirelle.adicrafter.crafter

import dev.adirelle.adicrafter.AdiCrafter
import dev.adirelle.adicrafter.crafter.internal.IngredientMatcher
import dev.adirelle.adicrafter.utils.minecraft.extensions.EMPTY_ITEM_AMOUNT
import dev.adirelle.adicrafter.utils.minecraft.extensions.toAmount
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant
import net.fabricmc.fabric.api.transfer.v1.storage.base.ResourceAmount
import net.minecraft.recipe.CraftingRecipe
import net.minecraft.util.Identifier
import java.util.function.Predicate

interface Recipe {

    val id: Identifier
    val output: ResourceAmount<ItemVariant>
    val ingredients: List<Ingredient>

    val isEmpty: Boolean

    data class Ingredient(val matcher: Predicate<ItemVariant>, val amount: Long)
}

abstract class AbstractRecipe(recipe: CraftingRecipe) : Recipe {

    override val id: Identifier = recipe.id

    override val output = recipe.output.toAmount()

    override val isEmpty = false

    override fun toString() = id.toString()

    override fun equals(other: Any?) =
        other is Recipe &&
            id == other.id &&
            ingredients.size == other.ingredients.size &&
            ingredients.withIndex().all { (index, ingredient) -> other.ingredients[index] == ingredient }

    override fun hashCode() = id.hashCode() + 31 * ingredients.hashCode()
}

class ExactRecipe(recipe: CraftingRecipe, grid: List<ItemVariant>) : AbstractRecipe(recipe) {

    override val ingredients: List<Recipe.Ingredient> =
        grid
            .filter { !it.isBlank }
            .groupBy { it }
            .map { (variant, list) ->
                Recipe.Ingredient(IngredientMatcher.exactly(variant), list.size.toLong())
            }
}

class FuzzyRecipe(recipe: CraftingRecipe) : AbstractRecipe(recipe) {

    override val ingredients: List<Recipe.Ingredient> =
        recipe.ingredients
            .filter { !it.isEmpty }
            .groupBy { it }
            .map { (ingredient, list) ->
                Recipe.Ingredient(IngredientMatcher.byTag(ingredient), list.size.toLong())
            }
}

object EMPTY_RECIPE : Recipe {

    override val id = Identifier(AdiCrafter.MOD_ID, "empty")
    override val output = EMPTY_ITEM_AMOUNT
    override val ingredients = listOf<Recipe.Ingredient>()
    override val isEmpty = true

    override fun toString() = "EMPTY_RECIPE"
    override fun equals(other: Any?) = (other as? Recipe)?.isEmpty ?: false
    override fun hashCode() = toString().hashCode()
}
