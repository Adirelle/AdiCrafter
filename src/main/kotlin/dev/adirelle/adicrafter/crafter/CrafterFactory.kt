@file:Suppress("UnstableApiUsage")

package dev.adirelle.adicrafter.crafter

import dev.adirelle.adicrafter.crafter.api.power.PowerSource
import dev.adirelle.adicrafter.crafter.api.storage.ResourceType
import dev.adirelle.adicrafter.crafter.api.storage.SingleTypeStorageProvider
import dev.adirelle.adicrafter.crafter.api.storage.StorageProvider
import dev.adirelle.adicrafter.crafter.impl.power.BasicGenerator
import dev.adirelle.adicrafter.crafter.impl.power.FueledGenerator
import dev.adirelle.adicrafter.crafter.impl.power.IllimitedGenerator
import dev.adirelle.adicrafter.crafter.impl.power.RedstoneGenerator
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

    val basic: BlockFactory by lazy {
        with(config.basic) {
            if (enabled)
                object : AbstractBlockFactory() {
                    override fun createGenerator(): PowerSource =
                        if (usePower) BasicGenerator(capacity, reloadRate)
                        else IllimitedGenerator
                }
            else BlockFactory.Disabled
        }
    }

    val fueled: BlockFactory by lazy {
        with(config.fueled) {
            if (enabled)
                object : AbstractBlockFactory() {
                    override fun createGenerator(): PowerSource =
                        FueledGenerator(capacity, reloadRate, powerPerBurningTick)
                } else BlockFactory.Disabled
        }
    }

    val redstone: BlockFactory by lazy {
        if (config.redstone.enabled)
            object : AbstractBlockFactory() {
                override fun createGenerator(): PowerSource =
                    with(config.redstone) {
                        RedstoneGenerator(powerPerDust)
                    }
            }
        else BlockFactory.Disabled
    }

    interface BlockFactory {

        val block: CrafterBlock?
        val blockEntityType: BlockEntityType<CrafterBlockEntity>?

        companion object Disabled : BlockFactory {

            override val block: CrafterBlock? = null
            override val blockEntityType: BlockEntityType<CrafterBlockEntity>? = null
        }

    }

    abstract class AbstractBlockFactory : BlockFactory {

        final override val block = CrafterBlock(::createBlockEntity)
        final override val blockEntityType = BlockEntityType(::createBlockEntity, setOf(block), null)

        protected abstract fun createGenerator(): PowerSource

        private fun createBlockEntity(pos: BlockPos, state: BlockState): CrafterBlockEntity {
            val generator = createGenerator()
            return CrafterBlockEntity(blockEntityType, pos, state, generator, FactoryImpl::with)
            { world, _ -> createStorageProvider(world, pos, generator) }
        }

        private fun createStorageProvider(
            world: World?,
            pos: BlockPos,
            powerSource: PowerSource
        ): StorageProvider =
            if (world is ServerWorld)
                StorageCompoundProvider(
                    buildMap {
                        put(ResourceType.ITEM, NeighborStorageProvider(ItemStorage.SIDED, world, pos))
                        put(ResourceType.FLUID, NeighborStorageProvider(FluidStorage.SIDED, world, pos))
                        put(ResourceType.POWER, SingleTypeStorageProvider.of(SingleViewStorage.of(powerSource)))
                    }
                )
            else
                StorageProvider.EMPTY
    }
}
