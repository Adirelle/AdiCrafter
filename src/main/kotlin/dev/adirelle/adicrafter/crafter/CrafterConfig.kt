package dev.adirelle.adicrafter.crafter

import kotlinx.serialization.Serializable

@Serializable
data class CrafterConfig(
    val craftPowerCost: Long = 160,
    val updatePeriod: Long = 4,
    val basic: BasicConfig = BasicConfig(),
    val fueled: FueledConfig = FueledConfig(),
    val redstone: RedstoneConfig = RedstoneConfig(),
    val energized: EnergizedConfig = EnergizedConfig()
) {

    @Serializable
    data class BasicConfig(
        val enabled: Boolean = true,
        val usePower: Boolean = true,
        val capacity: Long = 160,
        var reloadRate: Long = 16,
    )

    @Serializable
    data class FueledConfig(
        val enabled: Boolean = true,
        val capacity: Long = 4000,
        var reloadRate: Long = 16,
        var powerPerBurningTick: Double = 2.5
    )

    @Serializable
    data class RedstoneConfig(
        val enabled: Boolean = true,
        val powerPerDust: Long = 160
    )

    @Serializable
    data class EnergizedConfig(
        val enabled: Boolean = true,
        val capacity: Long = 4000,
        val transferRate: Long = 32,
    )
}
