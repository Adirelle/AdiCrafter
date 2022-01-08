@file:Suppress("UnstableApiUsage")

package dev.adirelle.adicrafter.utils.extension

import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant

fun ItemVariant.canCombineWith(other: ItemVariant) =
    this.toStack().canCombineWith(other)
