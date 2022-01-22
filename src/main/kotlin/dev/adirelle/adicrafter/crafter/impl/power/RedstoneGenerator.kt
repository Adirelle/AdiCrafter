package dev.adirelle.adicrafter.crafter.impl.power

import net.minecraft.item.Items

class RedstoneGenerator(
    powerPerDust: Long
) : ItemConsumerGenerator(
    mapOf(
        Items.REDSTONE to powerPerDust,
        Items.REDSTONE_BLOCK to 9 * powerPerDust
    )
)
