package dev.adirelle.adicrafter.crafter.impl.power

import net.minecraft.block.entity.AbstractFurnaceBlockEntity

class SolidFuelGenerator(
    capacity: Long,
    reloadRate: Long
) : ReloadingGenerator(
    capacity,
    reloadRate,
    ItemConsumerGenerator(
        AbstractFurnaceBlockEntity.createFuelTimeMap().mapValues { it.value.toLong() * reloadRate }
    )
)
