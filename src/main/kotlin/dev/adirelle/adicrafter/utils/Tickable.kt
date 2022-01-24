package dev.adirelle.adicrafter.utils

import net.minecraft.world.World

interface Tickable {

    fun tick(world: World): Boolean
}
