@file:Suppress("UnstableApiUsage")

package dev.adirelle.adicrafter.crafter.internal

import dev.adirelle.adicrafter.crafter.OptionalRecipe
import dev.adirelle.adicrafter.utils.extension.copyFrom
import dev.adirelle.adicrafter.utils.general.extensions.toStack
import dev.adirelle.adicrafter.utils.general.memoize
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.inventory.CraftingInventory
import net.minecraft.recipe.RecipeType
import net.minecraft.screen.ScreenHandler
import net.minecraft.world.World

class RecipeResolver private constructor(private val world: World) {

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

    fun resolve(grid: Grid): OptionalRecipe {
        craftingGrid.copyFrom(grid.map { it.toStack() })
        return OptionalRecipe.of(
            world.recipeManager.getFirstMatch(RecipeType.CRAFTING, craftingGrid, world),
            grid.map { it.resource }
        )
    }
}
