@file:Suppress("UnstableApiUsage")

package dev.adirelle.adicrafter.crafter.recipe.ingredient

import net.fabricmc.fabric.api.transfer.v1.storage.TransferVariant

fun interface IngredientFactory<T, U : Ingredient<V>, V : TransferVariant<*>> {

    fun create(input: T, amount: Long): U

    fun create(inputs: Iterable<T>): Collection<U> =
        inputs
            .groupBy { it }
            .map { (input, list) -> create(input, list.size.toLong()) }
}
