@file:Suppress("UnstableApiUsage")

package dev.adirelle.adicrafter.crafter

import dev.adirelle.adicrafter.utils.areEqual
import dev.adirelle.adicrafter.utils.extension.toItemString
import dev.adirelle.adicrafter.utils.extension.toVariant
import dev.adirelle.adicrafter.utils.general.lazyLogger
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext
import net.minecraft.item.ItemStack
import net.minecraft.recipe.CraftingRecipe
import java.util.*
import kotlin.math.ceil

open class BulkCraftingStorage : SingleSlotStorage<ItemVariant> {

    private val logger by lazyLogger

    var recipe: Optional<CraftingRecipe> = Optional.empty()
        set(newRecipe) {
            if (Transaction.isOpen()) {
                throw IllegalStateException("do not change the recipe during a transaction")
            }
            if (!areEqual(recipe, newRecipe)) {
                field = newRecipe
                markDirty()
            }
        }

    val output: ItemStack
        get() = recipe.map { it.output }.orElse(ItemStack.EMPTY)

    protected open fun markDirty() {}

    override fun extract(resource: ItemVariant, maxAmount: Long, transaction: TransactionContext?) =
        recipe.map {
            val crafted = craft(it, resource, maxAmount)
            logger.info("crafted {} item(s), request: {} {}", crafted, maxAmount, toItemString(resource))
            crafted
        }.orElse(0L)

    private fun craft(recipe: CraftingRecipe, resource: ItemVariant, maxAmount: Long) =
        if (maxAmount < 1 || !resource.matches(recipe.output)) 0L
        else recipe.output.count.let { it * ceil(maxAmount.toDouble() / it).toLong() }

    override fun isResourceBlank() = recipe.isEmpty
    override fun getResource(): ItemVariant = output.toVariant()
    override fun getAmount(): Long = output.count.toLong()
    override fun getCapacity() = output.item.maxCount.toLong()

    override fun supportsInsertion() = false
    override fun insert(resource: ItemVariant, maxAmount: Long, transaction: TransactionContext?) = 0L

}

