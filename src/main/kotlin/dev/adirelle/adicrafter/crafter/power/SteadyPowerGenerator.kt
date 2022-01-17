@file:Suppress("UnstableApiUsage")

package dev.adirelle.adicrafter.crafter.power

import dev.adirelle.adicrafter.crafter.power.PowerVariant.INSTANCE
import dev.adirelle.adicrafter.utils.Tickable
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleVariantStorage
import net.minecraft.nbt.NbtCompound
import net.minecraft.server.world.ServerWorld

class SteadyPowerGenerator(
    val maxCapacity: Long,
    val reloadRate: Long
) : SingleVariantStorage<PowerVariant>(), PowerGenerator, Tickable {

    companion object {

        private const val POWER_NBT_KEY = "Power"
    }

    override var onUpdate: () -> Unit = {}

    override fun onFinalCommit() {
        onUpdate()
    }

    override fun getCapacity(variant: PowerVariant) = maxCapacity
    override fun getBlankVariant() = INSTANCE

    private var lastTime = -1L

    override fun tick(world: ServerWorld) {
        val previousTime = lastTime
        lastTime = world.time
        if (previousTime > 0) {
            amount += reloadRate * (lastTime - previousTime)
            onUpdate()
        }
    }

    override fun readFromNbt(nbt: NbtCompound) {
        amount = nbt.getLong(POWER_NBT_KEY)
        onUpdate()
    }

    override fun writeToNbt(nbt: NbtCompound) {
        nbt.putLong(POWER_NBT_KEY, amount)
    }
}
