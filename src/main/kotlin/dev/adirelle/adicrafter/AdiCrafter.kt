package dev.adirelle.adicrafter

import dev.adirelle.adicrafter.crafter.CrafterFeature
import dev.adirelle.adicrafter.utils.Mod

object AdiCrafter : Mod("adicrafter") {

    val config = Config.loadOrCreate()

    override fun onInitialize() {
        feature(CrafterFeature)
    }
}
