@file:Suppress("UnstableApiUsage")

package dev.adirelle.adicrafter.crafter.recipe

import dev.adirelle.adicrafter.crafter.Recipe
import dev.adirelle.adicrafter.crafter.recipe.ingredient.Ingredient
import dev.adirelle.adicrafter.utils.extensions.copyFrom
import dev.adirelle.adicrafter.utils.lazyLogger
import dev.adirelle.adicrafter.utils.memoize
import dev.adirelle.adicrafter.utils.toItemString
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

    private val logger by lazyLogger

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
        logger.info("grid:")
        for ((idx, stack) in grid.withIndex()) {
            if (stack.isEmpty) continue
            logger.info("  #{}: {} -> {}", idx, stack.item.toItemString(), stack.item.recipeRemainder?.toItemString())
        }
        return world.recipeManager
            .getFirstMatch(RecipeType.CRAFTING, craftingGrid, world)
            .filter { !it.isEmpty }
            .map {
                logger.info("recipe ({}) ingredients:", it.id)
                for ((idx, ingredient) in it.ingredients.withIndex()) {
                    if (ingredient.isEmpty) continue
                    logger.info(" #{}", idx)
                    for (stack in ingredient.matchingStacks) {
                        logger.info("   {} -> {}", stack.toItemString(), stack.item.recipeRemainder?.toItemString())
                    }
                }
                val ingredients = factory.create(it.ingredients.filterNot { it.isEmpty }, grid.filterNot { it.isEmpty })
                logger.info("result ingredients:")
                for (ingredient in ingredients) {
                    logger.info("  {}", ingredient)
                }
                Recipe(it.id, it.output, ingredients)
            }
            .orElse(Recipe.EMPTY)
    }

    fun interface IngredientFactory {

        fun create(
            ingredients: Iterable<MinecraftIngredient>,
            grid: Iterable<ItemStack>
        ): Collection<Ingredient<*>>
    }

}
