@file:Suppress("UnstableApiUsage")

package dev.adirelle.adicrafter.utils.extension

import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant
import net.fabricmc.fabric.api.transfer.v1.storage.base.ResourceAmount
import net.minecraft.item.ItemStack

fun canExtract(request: ItemVariant, available: ItemVariant) =
    !available.isBlank && (request.isBlank || ItemStack.canCombine(available.toStack(), request.toStack()))

fun ItemVariant.toAmount(amount: Long) =
    ResourceAmount(this, amount)
