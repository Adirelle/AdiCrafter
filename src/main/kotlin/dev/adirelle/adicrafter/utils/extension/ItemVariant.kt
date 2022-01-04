@file:Suppress("UnstableApiUsage")

package dev.adirelle.adicrafter.utils.extension

import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant
import net.minecraft.item.ItemStack

val BLANK_ITEM_VARIANT: ItemVariant = ItemVariant.blank()

fun ItemVariant.canCombineWith(other: ItemAmount) = canCombineWith(other.toStack())
fun ItemVariant.canCombineWith(other: ItemVariant) = canCombineWith(other.toStack())
fun ItemVariant.canCombineWith(other: ItemStack) = ItemStack.canCombine(toStack(), other)
fun ItemVariant.getMaxCount(): Int = item.maxCount
