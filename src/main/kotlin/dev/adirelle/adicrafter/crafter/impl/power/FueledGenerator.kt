package dev.adirelle.adicrafter.crafter.impl.power

import dev.adirelle.adicrafter.crafter.api.power.PowerSource.Listener
import dev.adirelle.adicrafter.utils.memoize
import net.minecraft.block.entity.AbstractFurnaceBlockEntity

class FueledGenerator(
    capacity: Long,
    reloadRate: Long,
    powerPerBurningTick: Double,
    listener: Listener
) : ReloadingGenerator(
    capacity,
    reloadRate,
    ItemConsumerGenerator(memoizedFuelMap(powerPerBurningTick), listener),
    listener
) {

    companion object {

        private val fuelBurningTimes = AbstractFurnaceBlockEntity.createFuelTimeMap()

        private fun createFuelMap(powerPerBurningTick: Double) =
            fuelBurningTimes.mapValues { (it.value * powerPerBurningTick).toLong() }

        private val memoizedFuelMap = memoize(::createFuelMap)
    }
}
