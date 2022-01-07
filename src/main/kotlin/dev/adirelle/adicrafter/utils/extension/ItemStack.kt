@file:Suppress("UnstableApiUsage")

package dev.adirelle.adicrafter.utils.extension

import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NbtCompound
import net.minecraft.nbt.NbtList

fun ItemStack.toVariant(): ItemVariant =
    ItemVariant.of(this)

fun ItemStack.toNbt() =
    NbtCompound().also { writeNbt(it) }

fun Iterable<ItemStack>.toNbt() =
    NbtList().also { nbt ->
        this.forEach { nbt.add(it.toNbt()) }
    }

fun ItemStack.canCombineWith(other: ItemStack) =
    isEmpty || other.isEmpty || ItemStack.canCombine(this, other)

fun ItemStack.canCombineWith(other: ItemVariant) =
    this.canCombineWith(other.toStack())
