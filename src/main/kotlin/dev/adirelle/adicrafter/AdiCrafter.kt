package dev.adirelle.adicrafter

import dev.adirelle.adicrafter.crafter.Crafter
import dev.adirelle.adicrafter.utils.mod.Mod

object AdiCrafter : Mod("adicrafter") {

    override fun onInitialize() {
        feature(Crafter)
    }
}
