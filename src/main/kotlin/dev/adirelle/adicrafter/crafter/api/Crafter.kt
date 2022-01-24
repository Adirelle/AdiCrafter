@file:Suppress("UnstableApiUsage")

package dev.adirelle.adicrafter.crafter.api

import dev.adirelle.adicrafter.crafter.api.recipe.ItemIngredient
import dev.adirelle.adicrafter.utils.Droppable
import dev.adirelle.adicrafter.utils.NbtSerializable
import dev.adirelle.adicrafter.utils.ReadonlyInventory
import dev.adirelle.adicrafter.utils.storage.SingleViewStorage
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ItemConvertible
import net.minecraft.item.ItemStack
import org.apache.logging.log4j.LogManager
import java.util.*

interface Crafter : SingleViewStorage<ItemVariant>, ReadonlyInventory, NbtSerializable, Droppable {

    override fun size() = 1
    override fun isEmpty() = isResourceBlank
    override fun getStack(slot: Int): ItemStack {
        require(slot == 0)
        return resource.toStack(amount.toInt()).also {
            LogManager.getLogger().info("Crafter::getStack: {}", it)
        }
    }

    override fun canPlayerUse(player: PlayerEntity) =
        !player.isSpectator && player.abilities.allowModifyWorld

    fun findIngredientFor(item: ItemConvertible): Optional<ItemIngredient>

    fun interface Listener {

        fun onCrafterUpdate()
    }

    companion object EMPTY : Crafter {

        override fun extract(resource: ItemVariant, maxAmount: Long, transaction: TransactionContext) = 0L
        override fun isResourceBlank() = true
        override fun getResource(): ItemVariant = ItemVariant.blank()
        override fun getAmount() = 0L
        override fun getCapacity() = 0L
        override fun isEmpty() = true
        override fun getStack(slot: Int): ItemStack = ItemStack.EMPTY
        override fun findIngredientFor(item: ItemConvertible) = Optional.empty<ItemIngredient>()
    }
}
