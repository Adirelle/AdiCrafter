@file:Suppress("UnstableApiUsage")

package dev.adirelle.adicrafter.crafter.impl.power

import dev.adirelle.adicrafter.crafter.api.power.PowerSource

class BasicGenerator(capacity: Long, reloadRate: Long, listener: PowerSource.Listener) :
    ReloadingGenerator(capacity, reloadRate, IllimitedGenerator, listener)
