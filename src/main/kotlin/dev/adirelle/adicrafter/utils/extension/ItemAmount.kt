@file:Suppress("UnstableApiUsage")

package dev.adirelle.adicrafter.utils.extension

import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant
import net.fabricmc.fabric.api.transfer.v1.storage.base.ResourceAmount
import net.minecraft.item.ItemStack

typealias ItemAmount = ResourceAmount<ItemVariant>

val EMPTY_ITEM_AMOUNT: ItemAmount = ItemAmount(BLANK_ITEM_VARIANT, 0)

fun ItemAmount.toStack(toAmount: Int? = null) = ItemStack(resource.item, toAmount ?: amount.toInt())
fun ItemAmount.maxStackAmount() = if (resource.isBlank) 0L else resource.item.maxCount.toLong()
fun ItemAmount.canCombineWith(other: ItemAmount) = isEmpty() || resource.canCombineWith(other)
fun ItemAmount.canCombineWith(other: ItemVariant) = isEmpty() || resource.canCombineWith(other)
fun ItemAmount.canCombineWith(other: ItemStack) = isEmpty() || resource.canCombineWith(other)
