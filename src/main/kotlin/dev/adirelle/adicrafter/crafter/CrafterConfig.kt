package dev.adirelle.adicrafter.crafter

import kotlinx.serialization.Serializable

@Serializable
data class CrafterConfig(
    val power: PowerConfig = PowerConfig()
) {

    @Serializable
    data class PowerConfig(
        val enabled: Boolean = true,
        val capacity: Long = 1000L,
        val cost: Long = 100L,
        var reloadRate: Long = 10L
    )
}
