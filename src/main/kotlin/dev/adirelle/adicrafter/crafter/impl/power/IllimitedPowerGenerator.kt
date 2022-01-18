@file:Suppress("UnstableApiUsage")

package dev.adirelle.adicrafter.crafter.impl.power

import dev.adirelle.adicrafter.crafter.api.power.PowerGenerator
import dev.adirelle.adicrafter.crafter.api.power.PowerVariant
import dev.adirelle.adicrafter.crafter.api.power.PowerVariant.INSTANCE
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext

object IllimitedPowerGenerator : PowerGenerator {

    override fun extract(resource: PowerVariant, maxAmount: Long, transaction: TransactionContext) = maxAmount

    override fun isResourceBlank() = false
    override fun getResource() = INSTANCE
    override fun getAmount() = Long.MAX_VALUE
    override fun getCapacity() = 0L
}
