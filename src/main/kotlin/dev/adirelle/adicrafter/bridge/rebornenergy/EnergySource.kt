@file:Suppress("UnstableApiUsage")

package dev.adirelle.adicrafter.bridge.rebornenergy

import dev.adirelle.adicrafter.crafter.NbtKeys
import dev.adirelle.adicrafter.crafter.api.power.PowerSource
import dev.adirelle.adicrafter.crafter.api.power.PowerVariant
import dev.adirelle.adicrafter.utils.extensions.set
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext
import net.minecraft.nbt.NbtCompound
import team.reborn.energy.api.base.SimpleEnergyStorage
import kotlin.math.min

class EnergySource(
    capacity: Long,
    transferRate: Long,
    private val listener: PowerSource.Listener
) : PowerSource,
    SimpleEnergyStorage(capacity, transferRate, capacity) {

    override fun onFinalCommit() {
        listener.onPowerChanged()
    }

    override fun hasPowerBar() = true

    override fun extract(resource: PowerVariant, maxAmount: Long, tx: TransactionContext): Long =
        extract(maxAmount, tx)

    override fun readFromNbt(nbt: NbtCompound) {
        amount = min(nbt.getLong(NbtKeys.AMOUNT), capacity)
    }

    override fun writeToNbt(nbt: NbtCompound) {
        nbt[NbtKeys.AMOUNT] = amount
    }
}
