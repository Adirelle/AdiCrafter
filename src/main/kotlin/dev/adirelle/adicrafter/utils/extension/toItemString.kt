@file:Suppress("UnstableApiUsage")

package dev.adirelle.adicrafter.utils.extension

import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant
import net.minecraft.inventory.Inventory
import net.minecraft.item.ItemStack

fun toItemString(stack: ItemStack) =
    if (stack.isEmpty) "-" else stack.toString()

fun toItemString(variant: ItemVariant) =
    if (variant.isBlank) "-" else variant.item.toString()

fun toItemString(inv: Inventory) =
    toItemString(inv.toArray())

fun toItemString(arr: Array<ItemStack>) =
    toItemString(arr.asList())

fun toItemString(stacks: Iterable<ItemStack>) =
    if (stacks.all { it.isEmpty }) "[--]"
    else stacks.joinToString("|", "[", "]", transform = { toItemString(it) })
