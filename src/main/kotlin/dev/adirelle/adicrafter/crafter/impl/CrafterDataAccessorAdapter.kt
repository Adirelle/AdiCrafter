package dev.adirelle.adicrafter.crafter.impl

import dev.adirelle.adicrafter.crafter.api.CrafterDataAccessor
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.inventory.Inventory
import net.minecraft.item.ItemStack
import net.minecraft.screen.Property
import net.minecraft.screen.PropertyDelegate

class CrafterDataAccessorAdapter(
    private val backing: CrafterDataAccessor
) : CrafterDataAccessor by backing {

    companion object {

        const val GRID_FIRST_SLOT = 0
        const val GRID_LAST_SLOT = GRID_FIRST_SLOT + Grid.SIZE - 1
        const val FORECAST_SLOT = GRID_LAST_SLOT + 1
        const val RESULT_SLOT = FORECAST_SLOT + 1
        const val FUEL_SLOT = RESULT_SLOT + 1
        const val INVENTORY_SIZE = FUEL_SLOT + 1

        const val RECIPE_FLAGS_PROP = 0
        const val MISSING_PROP = 1
        const val POWER_AMOUNT_PROP = 2
        const val POWER_CAPACITY_PROP = 3
        const val PROP_COUNT = 4
    }

    val propertyDelegate = PropertyDelegateAdapter()

    val inventory = InventoryAdapter()

    inner class PropertyDelegateAdapter : PropertyDelegate {

        private inline fun <T> withProperty(index: Int, block: Property.() -> T): T =
            when (index) {
                RECIPE_FLAGS_PROP   -> recipeFlags
                MISSING_PROP        -> missingIngredients
                POWER_AMOUNT_PROP   -> powerAmount
                POWER_CAPACITY_PROP -> powerCapacity
                else                -> throw IndexOutOfBoundsException()
            }.block()

        override fun get(index: Int) =
            withProperty(index) { get() }

        override fun set(index: Int, value: Int) {
            withProperty(index) { set(value) }
        }

        override fun size() = PROP_COUNT

    }

    inner class InventoryAdapter : Inventory {

        private val inventories = listOf(grid, forecast, result)

        override fun size() = INVENTORY_SIZE

        override fun clear() {
            inventories.forEach { it.clear() }
        }

        override fun isEmpty() =
            inventories.all { it.isEmpty }

        override fun markDirty() {
            inventories.forEach { it.markDirty() }
        }

        override fun canPlayerUse(player: PlayerEntity) =
            inventories.any { it.canPlayerUse(player) }

        override fun onOpen(player: PlayerEntity) {
            inventories.forEach { it.onOpen(player) }
        }

        override fun onClose(player: PlayerEntity) {
            inventories.forEach { it.onClose(player) }
        }

        private inline fun <T> withInventory(index: Int, default: T, block: Inventory.(Int) -> T): T =
            when (index) {
                in GRID_FIRST_SLOT..GRID_LAST_SLOT -> grid.block(index - GRID_FIRST_SLOT)
                FORECAST_SLOT                      -> forecast.block(0)
                RESULT_SLOT                        -> result.block(0)
                FUEL_SLOT                          -> fuel?.block(0) ?: default
                else                               -> default
            }

        private inline fun withInventory(index: Int, block: Inventory.(Int) -> Unit) {
            withInventory(index, Unit, block)
        }

        override fun getStack(slot: Int): ItemStack =
            withInventory(slot, ItemStack.EMPTY) { getStack(it) }

        override fun removeStack(slot: Int, amount: Int): ItemStack =
            withInventory(slot, ItemStack.EMPTY) { removeStack(it, amount) }

        override fun removeStack(slot: Int): ItemStack =
            withInventory(slot, ItemStack.EMPTY) { removeStack(it) }

        override fun setStack(slot: Int, stack: ItemStack) {
            withInventory(slot) { setStack(it, stack) }
        }

    }

}
