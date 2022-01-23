package dev.adirelle.adicrafter.crafter

import kotlinx.serialization.Serializable

@Serializable
data class CrafterConfig(
    val craftPowerCost: Long = 200,
    val basic: BasicConfig = BasicConfig(),
    val fueled: FueledConfig = FueledConfig(),
    val redstone: RedstoneConfig = RedstoneConfig()
) {

    @Serializable
    data class BasicConfig(
        val enabled: Boolean = true,
        val usePower: Boolean = true,
        val capacity: Long = 2000,
        var reloadAmount: Long = 5,
        var reloadPeriod: Long = 3,
    )

    @Serializable
    data class FueledConfig(
        val enabled: Boolean = true,
        val capacity: Long = 4000,
        var reloadAmount: Long = 5,
        var reloadPeriod: Long = 2
    )

    @Serializable
    data class RedstoneConfig(
        val enabled: Boolean = true,
        val powerPerDust: Long = 200
    )
}
