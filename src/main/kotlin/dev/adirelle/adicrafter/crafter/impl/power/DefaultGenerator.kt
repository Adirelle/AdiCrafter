@file:Suppress("UnstableApiUsage")

package dev.adirelle.adicrafter.crafter.impl.power

class DefaultGenerator(capacity: Long, reloadAmount: Long, reloadPeriod: Long) :
    ReloadingGenerator(capacity, reloadAmount, reloadPeriod, IllimitedGenerator)
