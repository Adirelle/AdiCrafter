package dev.adirelle.adicrafter.crafter.api.recipe

import net.minecraft.item.ItemStack
import net.minecraft.recipe.CraftingRecipe
import net.minecraft.world.World
import java.util.*

fun interface Finder {

    fun find(world: World?, grid: List<ItemStack>): Optional<CraftingRecipe>
}
