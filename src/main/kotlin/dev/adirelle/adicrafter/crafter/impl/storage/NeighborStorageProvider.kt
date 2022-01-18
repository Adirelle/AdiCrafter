@file:Suppress("UnstableApiUsage")

package dev.adirelle.adicrafter.crafter.impl.storage

import dev.adirelle.adicrafter.crafter.api.storage.SingleTypeStorageProvider
import net.fabricmc.fabric.api.lookup.v1.block.BlockApiCache
import net.fabricmc.fabric.api.lookup.v1.block.BlockApiLookup
import net.fabricmc.fabric.api.transfer.v1.storage.Storage
import net.fabricmc.fabric.api.transfer.v1.storage.TransferVariant
import net.fabricmc.fabric.api.transfer.v1.storage.base.CombinedStorage
import net.minecraft.server.world.ServerWorld
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import java.util.function.Supplier

class NeighborStorageProvider<T : TransferVariant<*>>(
    lookup: BlockApiLookup<Storage<T>, Direction>,
    world: ServerWorld,
    pos: BlockPos
) : SingleTypeStorageProvider<T> {

    private val caches: List<Supplier<Storage<T>?>> = buildList {
        for (dir in Direction.values()) {
            val cache = BlockApiCache.create(lookup, world, pos.offset(dir))
            add { cache.find(dir.opposite) }
        }
    }

    override fun getStorage(): Storage<T> {
        return CombinedStorage(caches.mapNotNull { it.get() })
    }
}
