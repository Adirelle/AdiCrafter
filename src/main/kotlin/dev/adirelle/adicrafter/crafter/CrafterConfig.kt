package dev.adirelle.adicrafter.crafter

import kotlinx.serialization.Serializable

@Serializable
data class CrafterConfig(
    val usePower: Boolean = true,
    val craftPowerCost: Long = 200L,
    val basic: BasicConfig = BasicConfig(),
    val solidFuel: SolidfuelConfig = SolidfuelConfig()
    /*,
    val redstone: RedstoneConfig = RedstoneConfig(),
    */
) {

    @Serializable
    data class BasicConfig(
        val enabled: Boolean = true,
        val capacity: Long = 2000L,
        var reloadRate: Long = 20L
    )

    @Serializable
    data class SolidfuelConfig(
        val enabled: Boolean = true,
        val capacity: Long = 8000L,
        var reloadRate: Long = 5L
    )

    /*
    @Serializable
    data class RedstoneConfig(
        val enabled: Boolean = true,
        val powerPerDust: Long = 200L
    )
    */
}
