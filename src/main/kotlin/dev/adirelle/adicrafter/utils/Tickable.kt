package dev.adirelle.adicrafter.utils

import net.minecraft.world.World

interface Tickable {

    fun tick(world: World): Boolean
}

fun Iterable<Tickable>.tick(world: World): Boolean {
    var ticked = false
    forEach {
        ticked = it.tick(world) || ticked
    }
    return ticked
}
