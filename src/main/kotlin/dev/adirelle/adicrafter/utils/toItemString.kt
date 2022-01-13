@file:Suppress("UnstableApiUsage")

package dev.adirelle.adicrafter.utils

import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant
import net.minecraft.item.Item
import net.minecraft.item.ItemStack

fun Item.toItemString() = toString()
fun ItemStack.toItemString() = toString()
fun ItemVariant.toItemString() = item.toItemString()
