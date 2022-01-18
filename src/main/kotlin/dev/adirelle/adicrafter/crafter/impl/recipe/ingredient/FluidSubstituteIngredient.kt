@file:Suppress("UnstableApiUsage")

package dev.adirelle.adicrafter.crafter.impl.recipe.ingredient

import dev.adirelle.adicrafter.crafter.api.recipe.FluidIngredient
import dev.adirelle.adicrafter.crafter.api.recipe.IngredientFactory.ItemIngredientFactory
import dev.adirelle.adicrafter.crafter.api.recipe.ItemIngredient
import dev.adirelle.adicrafter.crafter.api.storage.StorageProvider
import dev.adirelle.adicrafter.utils.withOuterTransaction
import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import java.util.*

class FluidSubstituteIngredient(
    private val fluid: FluidIngredient,
    private val item: ItemIngredient
) : ItemIngredient by item {

    override fun extractFrom(provider: StorageProvider, maxAmount: Long, tx: TransactionContext): Long {
        var extracted = fluid.extractFrom(provider, maxAmount * fluid.amount, tx) / fluid.amount
        if (extracted < maxAmount) {
            extracted += item.extractFrom(provider, maxAmount - extracted, tx)
        }
        return extracted
    }

    override fun toString() = "{%s/%s}".format(fluid, item)

    companion object Factory : ItemIngredientFactory {

        private val cache = Collections.synchronizedMap(HashMap<Item, Optional<FluidIngredient>>())

        override fun create(stack: ItemStack, amount: Long): ItemIngredient {
            val baseIngredient = ExactIngredient.create(stack, amount)
            return findFluidIngredient(stack)
                .map<ItemIngredient> { FluidSubstituteIngredient(it, baseIngredient) }
                .orElse(baseIngredient)
        }

        private fun findFluidIngredient(stack: ItemStack) =
            cache.computeIfAbsent(stack.item, this::findFluidIngredientInternal)

        private fun findFluidIngredientInternal(item: Item): Optional<FluidIngredient> =
            Optional.ofNullable(
                ContainerItemContext.withInitial(ItemStack(item, 1))
                    .find(FluidStorage.ITEM)
            ).flatMap { storage ->
                Optional.ofNullable(
                    withOuterTransaction { tx ->
                        storage.iterable(tx)
                            .singleOrNull { !it.isResourceBlank }
                    }
                )
            }.map { view ->
                ExactIngredient(view.resource, view.amount)
            }

    }
}
