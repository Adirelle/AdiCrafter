package dev.adirelle.adicrafter.utils.extension

import net.minecraft.inventory.Inventory
import net.minecraft.item.ItemStack

fun Inventory.toArray(): Array<ItemStack> =
    Array(size()) { idx -> getStack(idx).copy() }

fun Inventory.toList(): List<ItemStack> =
    List(size()) { idx -> getStack(idx).copy() }
