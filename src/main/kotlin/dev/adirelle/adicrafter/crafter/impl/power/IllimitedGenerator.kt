@file:Suppress("UnstableApiUsage")

package dev.adirelle.adicrafter.crafter.impl.power

import dev.adirelle.adicrafter.crafter.api.power.PowerSource
import dev.adirelle.adicrafter.crafter.api.power.PowerVariant
import dev.adirelle.adicrafter.utils.Listenable.Listener
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext

object IllimitedGenerator : PowerSource {

    override fun extract(resource: PowerVariant, maxAmount: Long, transaction: TransactionContext) = maxAmount
    override fun hasPowerBar() = false
    override fun getAmount() = Long.MAX_VALUE
    override fun getCapacity() = Long.MAX_VALUE
    override fun addListener(listener: Listener) {}
    override fun removeListener(listener: Listener) {}
}
