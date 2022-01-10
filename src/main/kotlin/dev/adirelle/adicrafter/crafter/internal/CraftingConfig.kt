package dev.adirelle.adicrafter.crafter.internal

import dev.adirelle.adicrafter.crafter.EMPTY_RECIPE
import dev.adirelle.adicrafter.crafter.Recipe
import dev.adirelle.adicrafter.crafter.internal.Grid.Companion
import dev.adirelle.adicrafter.utils.Observer
import dev.adirelle.adicrafter.utils.general.ObservableValueHolder
import dev.adirelle.adicrafter.utils.minecraft.NbtPersistable
import net.minecraft.nbt.NbtList
import net.minecraft.world.World

class CraftingConfig : NbtPersistable<NbtList> {

    private val gridHolder: ObservableValueHolder<Grid> =
        object : ObservableValueHolder<Grid>(Grid.EMPTY) {
            override fun onValueChanged(oldValue: Grid, newValue: Grid) {
                dirtyRecipe = true
            }
        }
    var grid by gridHolder

    private var dirtyRecipe = false
    private val recipeHolder = ObservableValueHolder<Recipe>(EMPTY_RECIPE)
    val recipe by recipeHolder

    fun cleanRecipe(world: World): Boolean =
        if (dirtyRecipe) {
            dirtyRecipe = false
            recipeHolder.set(RecipeResolver.of(world).resolve(grid, false))
        } else
            false

    fun observeGrid(callback: Observer<Grid>) = gridHolder.observeValue(callback)
    fun observeRecipe(callback: Observer<Recipe>) = recipeHolder.observeValue(callback)

    fun readFromNbt(nbt: NbtList) {
        gridHolder.readFromNbt(nbt, Companion::fromNbt, Grid.EMPTY)
    }

    override fun toNbt() = grid.toNbt()
}
