package dev.adirelle.adicrafter.crafter

import kotlinx.serialization.Serializable

@Serializable
data class CrafterConfig(
    val craftPowerCost: Long = 200L,
    val basic: BasicConfig = BasicConfig(),
    val solidFuel: SolidfuelConfig = SolidfuelConfig(),
    val redstone: RedstoneConfig = RedstoneConfig()
) {

    @Serializable
    data class BasicConfig(
        val usePower: Boolean = true,
        val capacity: Long = 2000L,
        var reloadRate: Long = 20L
    ) : Enableable()

    @Serializable
    data class SolidfuelConfig(
        val capacity: Long = 8000L,
        var reloadRate: Long = 5L
    ) : Enableable()

    @Serializable
    data class RedstoneConfig(
        val powerPerDust: Long = 200L
    ) : Enableable()

    abstract class Enableable {

        val enabled: Boolean = true

        inline fun <T : Any> ifEnabled(crossinline block: () -> T): T? =
            if (enabled) block() else null

        inline fun ifEnabled(crossinline block: () -> Unit) =
            if (enabled) block() else Unit
    }
}
