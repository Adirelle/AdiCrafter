@file:Suppress("UnstableApiUsage")

package dev.adirelle.adicrafter.crafter.impl

import dev.adirelle.adicrafter.crafter.api.Crafter
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext

class SimulatingCrafter(
    private val backing: Crafter
) : Crafter by backing {

    private var lastSimulation: Long = 0L

    override fun getAmount() = lastSimulation

    override fun simulateExtract(resource: ItemVariant, maxAmount: Long, tx: TransactionContext?): Long {
        lastSimulation = super.simulateExtract(resource, maxAmount, tx)
        return lastSimulation
    }

    override fun insert(resource: ItemVariant, maxAmount: Long, tx: TransactionContext): Long {
        return super.insert(resource, maxAmount, tx)
    }
}
