@file:Suppress("UnstableApiUsage")

package dev.adirelle.adicrafter.crafter

import dev.adirelle.adicrafter.crafter.api.power.PowerGenerator
import dev.adirelle.adicrafter.crafter.api.storage.ResourceType
import dev.adirelle.adicrafter.crafter.api.storage.SingleTypeStorageProvider
import dev.adirelle.adicrafter.crafter.api.storage.StorageProvider
import dev.adirelle.adicrafter.crafter.impl.power.DefaultGenerator
import dev.adirelle.adicrafter.crafter.impl.power.IllimitedGenerator
import dev.adirelle.adicrafter.crafter.impl.power.RedstoneGenerator
import dev.adirelle.adicrafter.crafter.impl.power.SolidFuelGenerator
import dev.adirelle.adicrafter.crafter.impl.recipe.FactoryImpl
import dev.adirelle.adicrafter.crafter.impl.storage.NeighborStorageProvider
import dev.adirelle.adicrafter.crafter.impl.storage.StorageCompoundProvider
import dev.adirelle.adicrafter.utils.storage.SingleViewStorage
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage
import net.minecraft.block.BlockState
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.server.world.ServerWorld
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World

class CrafterFactory(
    private val config: CrafterConfig
) {

    private val disabled by lazy {
        object : BlockFactory {
            override val block: CrafterBlock? = null
            override val blockEntityType: BlockEntityType<CrafterBlockEntity>? = null
        }
    }

    val basic: BlockFactory by lazy {
        if (config.basic.enabled)
            object : AbstractBlockFactory() {
                override fun createGenerator(): PowerGenerator =
                    with(config.basic) {
                        if (usePower) DefaultGenerator(capacity, reloadRate)
                        else IllimitedGenerator
                    }
            }
        else disabled
    }

    val fueled: BlockFactory by lazy {
        if (config.solidFuel.enabled)
            object : AbstractBlockFactory() {
                override fun createGenerator(): PowerGenerator =
                    with(config.solidFuel) {
                        SolidFuelGenerator(capacity, reloadRate)
                    }
            } else disabled
    }

    val redstone: BlockFactory by lazy {
        if (config.redstone.enabled)
            object : AbstractBlockFactory() {
                override fun createGenerator(): PowerGenerator =
                    with(config.redstone) {
                        RedstoneGenerator(powerPerDust)
                    }
            }
        else disabled
    }

    interface BlockFactory {

        val block: CrafterBlock?
        val blockEntityType: BlockEntityType<CrafterBlockEntity>?
    }

    abstract class AbstractBlockFactory : BlockFactory {

        final override val block = CrafterBlock(::createBlockEntity)
        final override val blockEntityType = BlockEntityType(::createBlockEntity, setOf(block), null)

        protected abstract fun createGenerator(): PowerGenerator

        private fun createBlockEntity(pos: BlockPos, state: BlockState): CrafterBlockEntity {
            val generator = createGenerator()
            return CrafterBlockEntity(blockEntityType, pos, state, generator, FactoryImpl::with)
            { world, _ -> createStorageProvider(world, pos, generator) }
        }

        private fun createStorageProvider(
            world: World?,
            pos: BlockPos,
            powerGenerator: PowerGenerator
        ): StorageProvider =
            if (world is ServerWorld)
                StorageCompoundProvider(
                    buildMap {
                        put(ResourceType.ITEM, NeighborStorageProvider(ItemStorage.SIDED, world, pos))
                        put(ResourceType.FLUID, NeighborStorageProvider(FluidStorage.SIDED, world, pos))
                        put(ResourceType.POWER, SingleTypeStorageProvider.of(SingleViewStorage.of(powerGenerator)))
                    }
                )
            else
                StorageProvider.EMPTY
    }
}
