@file:Suppress("UnstableApiUsage")

package dev.adirelle.adicrafter.crafter.impl.power

import dev.adirelle.adicrafter.crafter.api.power.PowerGenerator
import dev.adirelle.adicrafter.crafter.api.power.PowerVariant
import dev.adirelle.adicrafter.crafter.api.power.PowerVariant.INSTANCE
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleVariantStorage
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext
import net.minecraft.nbt.NbtCompound
import net.minecraft.world.World
import kotlin.math.min

class SteadyPowerGenerator(
    private val capacityProp: Long,
    private val reloadRate: Long
) : SingleVariantStorage<PowerVariant>(), PowerGenerator {

    companion object {

        private const val POWER_NBT_KEY = "Power"
    }

    private var onUpdate: () -> Unit = {}

    override fun setUpdateCallback(callback: () -> Unit) {
        onUpdate = callback
    }

    override fun onFinalCommit() {
        onUpdate()
    }

    override fun isActive() = capacityProp > 0
    override fun getCapacity(variant: PowerVariant) = capacityProp
    override fun getBlankVariant() = INSTANCE

    private var lastTime = -1L

    override fun tick(world: World): Boolean {
        val previousTime = lastTime
        lastTime = world.time
        if (previousTime > 0 && amount < capacity) {
            amount = min(capacity, amount + reloadRate * (lastTime - previousTime))
            onUpdate()
            return true
        }
        return false
    }

    override fun readFromNbt(nbt: NbtCompound) {
        amount = nbt.getLong(POWER_NBT_KEY)
        onUpdate()
    }

    override fun writeToNbt(nbt: NbtCompound) {
        nbt.putLong(POWER_NBT_KEY, amount)
    }

    override fun insert(resource: PowerVariant, maxAmount: Long, transaction: TransactionContext?) = 0L
}
