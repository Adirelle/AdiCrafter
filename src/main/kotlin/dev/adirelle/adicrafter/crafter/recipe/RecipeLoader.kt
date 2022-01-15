package dev.adirelle.adicrafter.crafter.recipe

import dev.adirelle.adicrafter.utils.extensions.set
import net.minecraft.inventory.Inventory
import net.minecraft.item.ItemStack
import net.minecraft.recipe.CraftingRecipe
import net.minecraft.recipe.ShapedRecipe
import net.minecraft.recipe.ShapelessRecipe

class RecipeLoader(
    val inventory: Inventory,
    val startIndex: Int,
    val width: Int,
    val height: Int
) {

    fun load(recipe: ShapedRecipe) {
        for (x in 0 until width) {
            for (y in 0 until height) {
                inventory[startIndex + y * width + x] =
                    recipe.getIngredient(x, y)
            }
        }
    }

    fun load(recipe: ShapelessRecipe) {
        for (i in 0 until width * height) {
            inventory[startIndex + i] = recipe.getIngredient(i)
        }
    }

    private fun CraftingRecipe.getIngredient(i: Int): ItemStack =
        if (i < ingredients.size && !ingredients[i].isEmpty)
            ingredients[i].matchingStacks[0]
        else
            ItemStack.EMPTY

    private fun ShapedRecipe.getIngredient(x: Int, y: Int): ItemStack =
        if (x < width && y < height)
            getIngredient(y * width + x)
        else
            ItemStack.EMPTY
}
