@file:Suppress("UnstableApiUsage")

package dev.adirelle.adicrafter.crafter

import dev.adirelle.adicrafter.AdiCrafter
import dev.adirelle.adicrafter.crafter.recipe.ingredient.Ingredient
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant
import net.minecraft.item.ItemStack
import net.minecraft.item.ItemStack.areEqual
import net.minecraft.recipe.CraftingRecipe
import net.minecraft.util.Identifier

open class Recipe protected constructor(
    val id: Identifier,
    val output: ItemStack,
    val ingredients: List<Ingredient<ItemVariant>>,
) {

    open val isEmpty: Boolean = false

    override fun toString() = id.toString()

    override fun equals(other: Any?): Boolean {
        if (other === this) return true
        if (other?.javaClass != this.javaClass) return false
        other as Recipe

        return id == other.id &&
            areEqual(output, other.output) &&
            ingredients.size == other.ingredients.size &&
            ingredients.withIndex().all { (idx, ingredient) -> ingredient == other.ingredients[idx] }
    }

    override fun hashCode() =
        id.hashCode() * 31 + ingredients.hashCode()

    companion object {

        val EMPTY: Recipe = object : Recipe(
            Identifier(AdiCrafter.MOD_ID, "empty_recipe"),
            ItemStack.EMPTY,
            listOf()
        ) {
            override val isEmpty = true
            override fun toString() = "EMPTY_RECIPE"
            override fun equals(other: Any?) = (other as? Recipe)?.isEmpty ?: false
            override fun hashCode() = toString().hashCode()
        }

        fun of(recipe: CraftingRecipe, grid: List<ItemStack>, fuzzy: Boolean) =
            when {
                recipe.isEmpty -> EMPTY
                fuzzy          -> ofFuzzily(recipe)
                else           -> ofExactly(recipe, grid)
            }

        fun ofExactly(recipe: CraftingRecipe, grid: List<ItemStack>) =
            Recipe(
                recipe.id,
                recipe.output,
                grid
                    .filter { !it.isEmpty }
                    .groupBy { it.item }
                    .map { (item, stacks) -> Ingredient.exactly(item, stacks.size) }
            )

        fun ofFuzzily(recipe: CraftingRecipe) =
            Recipe(
                recipe.id,
                recipe.output,
                recipe.ingredients
                    .filter { !it.isEmpty }
                    .groupBy { it.matchingStacks }
                    .map { (stacks, list) -> Ingredient.anyOf(stacks, list.size) }
            )
    }
}

