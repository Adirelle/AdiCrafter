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

class NeighborStorageProvider<T, S : TransferVariant<T>>(
    override val type: ResourceType<T>,
    lookup: BlockApiLookup<Storage<S>, Direction>,
    world: ServerWorld,
    pos: BlockPos
) : SingleTypeStorageProvider<T> {

    private val caches: List<Supplier<Storage<S>?>> = buildList {
        for (dir in Direction.values()) {
            val cache = BlockApiCache.create(lookup, world, pos.offset(dir))
            add { cache.find(dir.opposite) }
        }
    }

    override fun <Z> getStorage(type: ResourceType<Z>): Storage<TransferVariant<Z>> {
        if (type != this.type) return CombinedStorage(listOf())
        val storage = CombinedStorage(caches.mapNotNull { it.get() })
        @Suppress("UNCHECKED_CAST")
        return storage as Storage<TransferVariant<Z>>
    }
}
