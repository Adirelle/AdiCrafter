@file:Suppress("UnstableApiUsage")

package dev.adirelle.adicrafter.crafter

import dev.adirelle.adicrafter.AdiCrafter
import dev.adirelle.adicrafter.utils.extension.toAmount
import dev.adirelle.adicrafter.utils.general.extensions.EMPTY_ITEM_AMOUNT
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant
import net.fabricmc.fabric.api.transfer.v1.storage.base.ResourceAmount
import net.minecraft.recipe.CraftingRecipe
import net.minecraft.util.Identifier
import java.util.*

interface OptionalRecipe {

    val id: Identifier
    val output: ResourceAmount<ItemVariant>
    val ingredients: List<ResourceAmount<ItemVariant>>

    val isEmpty: Boolean

    companion object {

        fun of(recipe: CraftingRecipe, grid: List<ItemVariant>) =
            SomeRecipe(recipe, grid)

        fun of(optionalRecipe: Optional<CraftingRecipe>, grid: List<ItemVariant>) =
            if (optionalRecipe.isPresent) of(optionalRecipe.get(), grid)
            else EMPTY_RECIPE
    }
}

class SomeRecipe(
    recipe: CraftingRecipe,
    grid: List<ItemVariant>
) : OptionalRecipe {

    override val id: Identifier = recipe.id

    override val output = recipe.output.toAmount()

    override val ingredients = grid
        .filter { !it.isBlank }
        .groupBy { it }
        .entries.map { entry -> ResourceAmount(entry.key, entry.value.size.toLong()) }

    override val isEmpty = false

    override fun toString() = id.toString()
    override fun equals(other: Any?) =
        other is SomeRecipe &&
            id == other.id &&
            ingredients.size == other.ingredients.size &&
            ingredients.withIndex().all { (index, amount) -> other.ingredients[index] == amount }

    override fun hashCode() = id.hashCode() + 31 * ingredients.hashCode()
}

object EMPTY_RECIPE : OptionalRecipe {

    override val id = Identifier(AdiCrafter.MOD_ID, "empty")
    override val output = EMPTY_ITEM_AMOUNT
    override val ingredients = listOf<ResourceAmount<ItemVariant>>()
    override val isEmpty = true

    override fun toString() = "EMPTY_RECIPE"
    override fun equals(other: Any?) = (other as? OptionalRecipe)?.isEmpty ?: false
    override fun hashCode() = toString().hashCode()
}
