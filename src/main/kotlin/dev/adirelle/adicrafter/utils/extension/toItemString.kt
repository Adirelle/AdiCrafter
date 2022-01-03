package dev.adirelle.adicrafter.utils.extension

import net.minecraft.inventory.Inventory
import net.minecraft.item.ItemStack

fun toItemString(stack: ItemStack) =
    if (stack.isEmpty) "-" else stack.toString()

fun toItemString(inv: Inventory) =
    toItemString(inv.toArray())

fun toItemString(arr: Array<ItemStack>) =
    toItemString(arr.asList())

fun toItemString(stacks: Iterable<ItemStack>) =
    if (stacks.all { it.isEmpty }) "[--]"
    else stacks.joinToString("|", "[", "]", transform = { toItemString(it) })
