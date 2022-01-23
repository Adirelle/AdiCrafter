package dev.adirelle.adicrafter.crafter.impl.power

import net.minecraft.block.entity.AbstractFurnaceBlockEntity
import java.util.*

class FueledGenerator(
    capacity: Long,
    reloadAmount: Long,
    reloadPeriod: Long
) : ReloadingGenerator(
    capacity,
    reloadAmount,
    reloadPeriod,
    ItemConsumerGenerator(
        { item ->
            Optional.ofNullable(fuelTimes[item]?.toLong()?.times(reloadAmount)?.div(reloadPeriod))
        }
    )
) {

    companion object {

        private val fuelTimes = AbstractFurnaceBlockEntity.createFuelTimeMap()

    }

}
