@file:Suppress("UnstableApiUsage")

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
    val generator: Inventory?

    @Suppress("UnstableApiUsage")
    val crafter: Storage<ItemVariant>

    val recipeFlags: Property
    val missingIngredients: Property

    val hasPowerBar: Boolean
    val powerAmount: Property
    val powerCapacity: Property

    fun onScreenHandlerClosed(handler: ScreenHandler)

    class Dummy(
        override val hasPowerBar: Boolean,
        hasGeneratorInventory: Boolean
    ) : CrafterDataAccessor {

        override val grid = SimpleInventory(Grid.SIZE)
        override var forecast = SimpleInventory(1)
        override val result = SimpleInventory(1)
        override val generator = if (hasGeneratorInventory) SimpleInventory(1) else null

        override val crafter = NullStorage(ItemVariant.blank())

        override var recipeFlags: Property = Property.create()
        override val missingIngredients: Property = Property.create()

        override val powerAmount: Property = Property.create()
        override val powerCapacity: Property = Property.create()

        override fun onScreenHandlerClosed(handler: ScreenHandler) {}
    }
}
