@file:Suppress("UnstableApiUsage")

package dev.adirelle.adicrafter.utils.extensions

import dev.adirelle.adicrafter.utils.toItemString
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant
import net.fabricmc.fabric.api.transfer.v1.storage.TransferVariant

fun <T : TransferVariant<*>> T.toItemString(): String =
    when (this) {
        is ItemVariant  -> toItemString()
        is FluidVariant -> fluid.javaClass.simpleName
        else            -> toString()
    }
