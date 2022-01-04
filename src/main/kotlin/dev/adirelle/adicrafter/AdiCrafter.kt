package dev.adirelle.adicrafter

import dev.adirelle.adicrafter.crafter.Crafter
import dev.adirelle.adicrafter.utils.mod.mod

val AdiCrafter = mod("adicrafter") {

    LOGGER.info("I was at places!")

    clientOnly {
        LOGGER.info("I was on the client!")
    }

    serverOnly {
        LOGGER.info("I was on the server!")
    }

    features {
        Crafter
    }
}
