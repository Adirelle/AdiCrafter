@file:Suppress("UnstableApiUsage")

package dev.adirelle.adicrafter.utils.extensions

import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NbtCompound

fun ItemStack.toVariant(): ItemVariant =
    if (isEmpty) ItemVariant.blank() else ItemVariant.of(this)

fun ItemStack.toNbt() =
    NbtCompound().also { writeNbt(it) }
