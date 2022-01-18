package dev.adirelle.adicrafter.crafter.impl.recipe

import dev.adirelle.adicrafter.AdiCrafter
import dev.adirelle.adicrafter.crafter.RecipeImpl
import dev.adirelle.adicrafter.crafter.api.recipe.Finder
import dev.adirelle.adicrafter.crafter.api.recipe.IngredientFactory
import dev.adirelle.adicrafter.crafter.api.recipe.Recipe
import dev.adirelle.adicrafter.crafter.api.recipe.RecipeFlags
import dev.adirelle.adicrafter.crafter.impl.recipe.ingredient.ExactIngredient
import dev.adirelle.adicrafter.crafter.impl.recipe.ingredient.FluidSubstituteIngredient
import dev.adirelle.adicrafter.utils.extensions.toVariant
import net.minecraft.item.ItemStack
import net.minecraft.world.World
import java.util.*

class FactoryImpl(
    private val finder: Finder,
    private val ingredientFactory: IngredientFactory
) : Recipe.Factory {

    override fun create(world: World?, grid: List<ItemStack>): Recipe =
        finder.find(world, grid)
            .map<Recipe> {
                with(it) {
                    RecipeImpl(
                        id,
                        output.toVariant(),
                        output.count.toLong(),
                        ingredientFactory.create(this, grid)
                    )
                }
            }
            .orElse(Recipe.EMPTY)

    companion object {

        private val powerCost: Long by lazy { AdiCrafter.config.crafter.power.cost }

        private val instances = Collections.synchronizedMap(HashMap<RecipeFlags, Recipe.Factory>())

        fun with(flags: RecipeFlags) =
            instances.computeIfAbsent(flags, ::create)

        private fun create(flags: RecipeFlags) =
            FactoryImpl(FinderImpl, createIngredientFactory(flags))

        private fun createIngredientFactory(flags: RecipeFlags): IngredientFactory {
            val itemIngredientFactory =
                if (RecipeFlags.FLUIDS in flags)
                    FluidSubstituteIngredient.Factory
                else
                    ExactIngredient.Factory

            val recipeIngredientFactory =
                if (RecipeFlags.FUZZY in flags)
                    FuzzyRecipeIngredientFactory(itemIngredientFactory)
                else
                    ExactRecipeIngredientFactory(itemIngredientFactory)

            return if (RecipeFlags.POWER in flags)
                PoweredRecipeIngredientFactory(recipeIngredientFactory, powerCost)
            else
                recipeIngredientFactory
        }

    }
}
