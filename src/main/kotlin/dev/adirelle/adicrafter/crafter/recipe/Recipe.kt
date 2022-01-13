@file:Suppress("UnstableApiUsage")

package dev.adirelle.adicrafter.crafter

import dev.adirelle.adicrafter.AdiCrafter
import dev.adirelle.adicrafter.crafter.recipe.ingredient.Ingredient
import dev.adirelle.adicrafter.utils.areEqual
import net.minecraft.item.ItemStack
import net.minecraft.util.Identifier

open class Recipe(
    val id: Identifier,
    val output: ItemStack,
    val ingredients: Collection<Ingredient<*>>,
) {

    open val isEmpty: Boolean = false

    override fun toString() = id.toString()

    override fun equals(other: Any?): Boolean {
        if (other === this) return true
        if (other?.javaClass != this.javaClass) return false
        other as Recipe

        return id == other.id &&
            areEqual(output, other.output) &&
            areEqual(ingredients, other.ingredients)
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
    }
}

