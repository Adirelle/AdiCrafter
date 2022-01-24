@file:Suppress("UnstableApiUsage")

package dev.adirelle.adicrafter.crafter.impl.power

class BasicGenerator(capacity: Long, reloadRate: Long) :
    ReloadingGenerator(capacity, reloadRate, IllimitedGenerator)
