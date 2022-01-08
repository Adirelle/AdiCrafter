package dev.adirelle.adicrafter.utils.minecraft

import dev.adirelle.adicrafter.utils.extension.asList
import dev.adirelle.adicrafter.utils.extension.copyFrom
import dev.adirelle.adicrafter.utils.general.memoize
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.inventory.CraftingInventory
import net.minecraft.inventory.Inventory
import net.minecraft.item.ItemStack
import net.minecraft.recipe.CraftingRecipe
import net.minecraft.recipe.Recipe
import net.minecraft.recipe.RecipeType
import net.minecraft.screen.ScreenHandler
import net.minecraft.world.World
import java.util.*

interface RecipeResolver<T : Recipe<*>> {

    fun resolve(stacks: List<ItemStack>): Optional<T>

    fun resolve(inventory: Inventory): Optional<T> =
        resolve(inventory.asList())

    fun resolve(ary: Array<ItemStack>): Optional<T> =
        resolve(ary.asList())
}

class CraftingRecipeResolver private constructor(private val world: World) :
    RecipeResolver<CraftingRecipe> {

    companion object {

        private val instances = memoize(::CraftingRecipeResolver)

        fun of(world: World) = instances[world]

        private val dummyScreenHandler: ScreenHandler by lazy {
            object : ScreenHandler(null, 0) {
                override fun canUse(player: PlayerEntity?) = false
            }
        }
    }

    private val craftingGrid = CraftingInventory(dummyScreenHandler, 3, 3)

    override fun resolve(stacks: List<ItemStack>): Optional<CraftingRecipe> {
        craftingGrid.copyFrom(stacks)
        return world.recipeManager.getFirstMatch(RecipeType.CRAFTING, craftingGrid, world)
    }
}

