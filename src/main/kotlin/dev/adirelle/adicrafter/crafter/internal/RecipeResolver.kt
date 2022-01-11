@file:Suppress("UnstableApiUsage")

package dev.adirelle.adicrafter.crafter.internal

import dev.adirelle.adicrafter.crafter.EMPTY_RECIPE
import dev.adirelle.adicrafter.crafter.ExactRecipe
import dev.adirelle.adicrafter.crafter.FuzzyRecipe
import dev.adirelle.adicrafter.crafter.Recipe
import dev.adirelle.adicrafter.utils.extensions.copyFrom
import dev.adirelle.adicrafter.utils.extensions.toStack
import dev.adirelle.adicrafter.utils.memoize
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.inventory.CraftingInventory
import net.minecraft.recipe.RecipeType
import net.minecraft.screen.ScreenHandler
import net.minecraft.world.World

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

    fun resolve(grid: Grid, fuzzy: Boolean): Recipe {
        craftingGrid.copyFrom(grid.map { it.toStack() })
        val result = world.recipeManager.getFirstMatch(RecipeType.CRAFTING, craftingGrid, world)
        return when {
            result.isEmpty -> EMPTY_RECIPE
            fuzzy          -> FuzzyRecipe(result.get())
            else           -> ExactRecipe(result.get(), grid.map { it.resource })
        }
    }
}
