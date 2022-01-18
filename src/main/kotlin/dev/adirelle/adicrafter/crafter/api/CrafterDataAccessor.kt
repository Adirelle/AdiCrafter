package dev.adirelle.adicrafter.crafter.api

import dev.adirelle.adicrafter.crafter.impl.Grid
import dev.adirelle.adicrafter.utils.storage.NullStorage
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant
import net.fabricmc.fabric.api.transfer.v1.storage.Storage
import net.minecraft.inventory.Inventory
import net.minecraft.inventory.SimpleInventory
import net.minecraft.screen.Property
import net.minecraft.screen.ScreenHandler

interface CrafterDataAccessor {

    val grid: Inventory
    val forecast: Inventory
    val result: Inventory

    @Suppress("UnstableApiUsage")
    val crafter: Storage<ItemVariant>

    val recipeFlags: Property
    val missingIngredients: Property
    val powerAmount: Property
    val powerCapacity: Property

    fun onScreenHandlerClosed(handler: ScreenHandler)

    companion object {

        fun empty(): CrafterDataAccessor =
            object : CrafterDataAccessor {
                override val grid = SimpleInventory(Grid.SIZE)
                override var forecast = SimpleInventory(1)
                override val result = SimpleInventory(1)

                @Suppress("UnstableApiUsage")
                override val crafter = NullStorage(ItemVariant.blank())

                override var recipeFlags = Property.create()
                override val missingIngredients = Property.create()
                override val powerAmount = Property.create()
                override val powerCapacity = Property.create()

                override fun onScreenHandlerClosed(handler: ScreenHandler) {}
            }
    }
}
