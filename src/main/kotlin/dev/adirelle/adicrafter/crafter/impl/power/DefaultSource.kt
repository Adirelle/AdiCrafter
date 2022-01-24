@file:Suppress("UnstableApiUsage")

package dev.adirelle.adicrafter.crafter.impl.power

class DefaultSource(capacity: Long, reloadAmount: Long, reloadPeriod: Long) :
    ReloadingGenerator(capacity, reloadAmount, reloadPeriod, IllimitedGenerator)
