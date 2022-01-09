@file:Suppress("UnstableApiUsage")

package dev.adirelle.adicrafter.crafter.internal

import net.fabricmc.fabric.api.lookup.v1.block.BlockApiCache
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant
import net.fabricmc.fabric.api.transfer.v1.storage.Storage
import net.minecraft.server.world.ServerWorld
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.world.World
import java.util.function.Supplier

typealias InputProvider = Supplier<List<Storage<ItemVariant>>?>

abstract class AbstractInputProvider : InputProvider {

    abstract val world: World?
    abstract val pos: BlockPos

    override fun get(): List<Storage<ItemVariant>>? =
        (world as? ServerWorld)?.let { getFromServerWorld(it) }

    private fun getFromServerWorld(world: ServerWorld): List<Storage<ItemVariant>> {
        val neighborApiCaches: Map<Direction, BlockApiCache<Storage<ItemVariant>, Direction>> by lazy {
            buildMap {
                for (dir in Direction.values()) {
                    put(dir, BlockApiCache.create(ItemStorage.SIDED, world, pos.offset(dir)))
                }
            }
        }

        return neighborApiCaches
            .mapNotNull { (dir, cache) -> cache.find(dir) }
            .filter { it.supportsExtraction() }
    }

}
