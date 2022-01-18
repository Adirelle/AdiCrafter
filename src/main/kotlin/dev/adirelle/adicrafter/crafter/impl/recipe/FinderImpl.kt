package dev.adirelle.adicrafter.crafter.impl.recipe

import dev.adirelle.adicrafter.crafter.api.recipe.Finder
import dev.adirelle.adicrafter.utils.extensions.copyFrom
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.inventory.CraftingInventory
import net.minecraft.item.ItemStack
import net.minecraft.recipe.CraftingRecipe
import net.minecraft.recipe.RecipeType
import net.minecraft.screen.ScreenHandler
import net.minecraft.server.world.ServerWorld
import net.minecraft.world.World
import java.util.*

object FinderImpl : Finder {

    private val craftingGrid by lazy {
        CraftingInventory(
            object : ScreenHandler(null, 0) {
                override fun canUse(player: PlayerEntity?) = false
            },
            3,
            3
        )
    }

    override fun find(world: World?, grid: List<ItemStack>): Optional<CraftingRecipe> =
        if (grid.isEmpty() || grid.all { it.isEmpty } || world !is ServerWorld)
            Optional.empty()
        else
            synchronized(craftingGrid) {
                craftingGrid.copyFrom(grid)
                world.recipeManager
                    .getFirstMatch(RecipeType.CRAFTING, craftingGrid, world)
                    .filter { !it.isEmpty }
            }
}
