package dev.adirelle.adicrafter.utils

import net.minecraft.item.ItemStack

interface Droppable {

    fun getDroppedStacks(): List<ItemStack> = emptyList()
}
