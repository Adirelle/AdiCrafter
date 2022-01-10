@file:Suppress("UnstableApiUsage")

package dev.adirelle.adicrafter.utils.minecraft

import dev.adirelle.adicrafter.utils.minecraft.extensions.asList
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant
import net.fabricmc.fabric.api.transfer.v1.storage.base.ResourceAmount
import net.minecraft.inventory.Inventory
import net.minecraft.item.Item
import net.minecraft.item.ItemStack

fun Item.toItemString() = toString()
fun ItemStack.toItemString() = toString()
fun ItemVariant.toItemString() = item.toItemString()
fun ResourceAmount<ItemVariant>.toItemString() = "%d %s".format(amount, resource.toItemString())

fun Inventory.toItemString() = asList().toItemString()
fun Array<ItemStack>.toItemString() = asList().toItemString()

fun Iterable<ItemStack>.toItemString(): String =
    if (all { it.isEmpty }) "[--]"
    else joinToString("|", "[", "]", transform = { it.toItemString() })
