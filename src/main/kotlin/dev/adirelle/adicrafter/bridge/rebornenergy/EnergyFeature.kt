package dev.adirelle.adicrafter.bridge.rebornenergy

import dev.adirelle.adicrafter.AdiCrafter
import dev.adirelle.adicrafter.crafter.CrafterFactory.AbstractBlockFactory
import dev.adirelle.adicrafter.crafter.CrafterFactory.BlockFactory
import dev.adirelle.adicrafter.crafter.CrafterFeature
import dev.adirelle.adicrafter.crafter.api.power.PowerSource
import dev.adirelle.adicrafter.utils.ModFeature
import team.reborn.energy.api.EnergyStorage

object EnergyFeature : ModFeature(AdiCrafter) {

    private val config = AdiCrafter.config.crafter.energized

    val factory = if (config.enabled)
        object : AbstractBlockFactory() {
            override fun createGenerator(): PowerSource =
                EnergySource(config.capacity, config.transferRate)
        }
    else
        BlockFactory.Disabled

    val ENERGY_CRAFTER by lazy { factory.block }
    val ENERGY_CRAFTER_ENTITY_TYPE by lazy { factory.blockEntityType }

    override fun onInitialize() {
        ENERGY_CRAFTER?.let { block ->
            CrafterFeature.registerBlock(id("energized_crafter"), block, ENERGY_CRAFTER_ENTITY_TYPE!!)

            EnergyStorage.SIDED.registerForBlockEntity(
                { blockEntity, _ -> blockEntity.powerSource as? EnergyStorage },
                ENERGY_CRAFTER_ENTITY_TYPE
            )

            LOGGER.info("Energized crafter initialized")
        }
    }
}
