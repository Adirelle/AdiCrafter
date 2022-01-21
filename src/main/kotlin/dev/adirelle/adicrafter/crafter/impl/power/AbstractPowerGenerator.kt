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

abstract class AbstractPowerGenerator(
    protected val _capacity: Long,
    protected val reloadRate: Long
) : SingleVariantStorage<PowerVariant>(), PowerGenerator {

    companion object {

        private const val POWER_NBT_KEY = "Power"
    }

    protected var onUpdate: () -> Unit = {}

    private var lastTime = -1L

    protected open fun reload(delay: Long, maxAmount: Long) =
        reloadRate * (delay)

    override fun hasPowerBar() = true
    
    override fun tick(world: World): Boolean {
        val previousTime = lastTime
        lastTime = world.time

        val maxAmount = capacity - amount
        if (previousTime <= 0 || maxAmount <= 0) return false

        val generated = min(maxAmount, reload(lastTime - previousTime, maxAmount))
        if (generated <= 0) return false

        amount += generated
        onUpdate()
        return true
    }

    override fun setUpdateCallback(callback: () -> Unit) {
        onUpdate = callback
    }

    override fun isActive() = capacity > 0
    override fun getBlankVariant() = INSTANCE

    override fun getCapacity(variant: PowerVariant) = _capacity

    override fun onFinalCommit() {
        onUpdate()
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
