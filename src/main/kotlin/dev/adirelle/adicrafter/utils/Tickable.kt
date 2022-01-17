package dev.adirelle.adicrafter.utils

import net.minecraft.server.world.ServerWorld

interface Tickable {

    fun tick(world: ServerWorld)
}
