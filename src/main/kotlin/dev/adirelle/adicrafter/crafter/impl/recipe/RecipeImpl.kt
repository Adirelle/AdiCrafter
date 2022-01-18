@file:Suppress("UnstableApiUsage")

package dev.adirelle.adicrafter.crafter

import dev.adirelle.adicrafter.crafter.api.recipe.Ingredient
import dev.adirelle.adicrafter.crafter.api.recipe.Recipe
import dev.adirelle.adicrafter.crafter.api.storage.StorageProvider
import dev.adirelle.adicrafter.crafter.impl.CrafterImpl
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant
import net.minecraft.util.Identifier

data class RecipeImpl(
    private val id: Identifier,
    private val resource: ItemVariant,
    private val amount: Long,
    private val ingredients: Collection<Ingredient<*>>,
) : Recipe {

    override fun toString() = id.toString()

    override fun createCrafter(provider: StorageProvider) =
        CrafterImpl(resource, amount, ingredients, provider)
}
