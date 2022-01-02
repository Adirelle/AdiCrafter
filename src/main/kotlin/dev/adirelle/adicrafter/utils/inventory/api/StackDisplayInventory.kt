package dev.adirelle.adicrafter.utils.inventory.api

import dev.adirelle.adicrafter.utils.inventory.impl.StackDisplayInventoryImpl
import net.minecraft.item.ItemStack

interface StackDisplayInventory : ReadonlyInventory {

    companion object {

        fun of(stack: ItemStack): StackDisplayInventory = StackDisplayInventoryImpl(stack)
    }

    var stack: ItemStack
}
