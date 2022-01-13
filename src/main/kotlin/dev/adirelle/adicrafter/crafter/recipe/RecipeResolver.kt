@file:Suppress("UnstableApiUsage")

package dev.adirelle.adicrafter.crafter.recipe

import dev.adirelle.adicrafter.crafter.Recipe
import dev.adirelle.adicrafter.crafter.recipe.ingredient.Ingredient
import dev.adirelle.adicrafter.utils.extensions.copyFrom
import dev.adirelle.adicrafter.utils.memoize
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.inventory.CraftingInventory
import net.minecraft.item.ItemStack
import net.minecraft.recipe.RecipeType
import net.minecraft.screen.ScreenHandler
import net.minecraft.world.World
import net.minecraft.recipe.Ingredient as MinecraftIngredient

class RecipeResolver private constructor(
    private val world: World
) {

    companion object {

        private val instances = memoize(::RecipeResolver)

        fun of(world: World) = instances[world]

        private val dummyScreenHandler: ScreenHandler by lazy {
            object : ScreenHandler(null, 0) {
                override fun canUse(player: PlayerEntity?) = false
            }
        }
    }

    private val craftingGrid by lazy { CraftingInventory(dummyScreenHandler, 3, 3) }

    fun resolve(grid: Grid, factory: IngredientFactory): Recipe {
        craftingGrid.copyFrom(grid.asList())
        val result = world.recipeManager.getFirstMatch(RecipeType.CRAFTING, craftingGrid, world)
        return result
            .filter { !it.isEmpty }
            .map { Recipe(it.id, it.output, factory.create(it.ingredients, grid)) }
            .orElse(Recipe.EMPTY)
    }

    fun interface IngredientFactory {

        fun create(
            ingredients: Iterable<MinecraftIngredient>,
            grid: Iterable<ItemStack>
        ): Collection<Ingredient<ItemVariant>>
    }
}
