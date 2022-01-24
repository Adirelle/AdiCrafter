package dev.adirelle.adicrafter

import dev.adirelle.adicrafter.bridge.rebornenergy.EnergyFeature
import dev.adirelle.adicrafter.crafter.CrafterFeature
import dev.adirelle.adicrafter.utils.Mod
import net.fabricmc.loader.api.FabricLoader

object AdiCrafter : Mod("AdiCrafter", "adicrafter") {

    val config = Config.loadOrCreate()

    override fun onInitialize() {
        feature(CrafterFeature)

        if (FabricLoader.getInstance().isModLoaded("team_reborn_energy")) {
            feature(EnergyFeature)
        }
    }
}
