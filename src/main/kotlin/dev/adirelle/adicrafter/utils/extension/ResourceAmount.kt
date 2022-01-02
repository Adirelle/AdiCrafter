@file:Suppress("UnstableApiUsage")

package dev.adirelle.adicrafter.utils.extension

import net.fabricmc.fabric.api.transfer.v1.storage.TransferVariant
import net.fabricmc.fabric.api.transfer.v1.storage.base.ResourceAmount

fun <T : TransferVariant<*>> ResourceAmount<T>.isEmpty() = resource.isBlank || amount < 1
fun <T : TransferVariant<*>> ResourceAmount<T>.withAmount(amount: Long) = ResourceAmount(resource, amount)
fun <T : TransferVariant<*>> ResourceAmount<T>.addAmount(delta: Long) = withAmount(amount + delta)
