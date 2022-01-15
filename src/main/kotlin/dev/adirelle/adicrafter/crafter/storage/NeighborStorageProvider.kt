@file:Suppress("UnstableApiUsage")

package dev.adirelle.adicrafter.crafter.storage

import net.fabricmc.fabric.api.lookup.v1.block.BlockApiCache
import net.fabricmc.fabric.api.lookup.v1.block.BlockApiLookup
import net.fabricmc.fabric.api.transfer.v1.storage.Storage
import net.fabricmc.fabric.api.transfer.v1.storage.TransferVariant
import net.fabricmc.fabric.api.transfer.v1.storage.base.CombinedStorage
import net.minecraft.server.world.ServerWorld
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import java.util.function.Supplier

class NeighborStorageProvider<T, V : TransferVariant<T>>(
    override val type: ResourceType<T>,
    lookup: BlockApiLookup<Storage<V>, Direction>,
    world: ServerWorld,
    pos: BlockPos
) : StorageCompoundProvider.SingleTypeStorageProvider<T, V> {

    private val caches: List<Supplier<Storage<V>?>> = buildList {
        for (dir in Direction.values()) {
            val cache = BlockApiCache.create(lookup, world, pos.offset(dir))
            add { cache.find(dir.opposite) }
        }
    }

    override fun getStorage(): Storage<V> {
        return CombinedStorage(caches.mapNotNull { it.get() })
    }
}
