package dev.adirelle.adicrafter

import dev.adirelle.adicrafter.crafter.Crafter
import dev.adirelle.adicrafter.utils.Mod

object AdiCrafter : Mod("adicrafter") {

    override fun onInitialize() {
        feature(Crafter)
    }
}
