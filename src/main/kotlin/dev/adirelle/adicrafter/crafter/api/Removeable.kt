package dev.adirelle.adicrafter.crafter.api

import net.minecraft.util.math.BlockPos
import net.minecraft.world.World

interface Removeable {

    fun onRemoved(world: World, pos: BlockPos) {}
}
