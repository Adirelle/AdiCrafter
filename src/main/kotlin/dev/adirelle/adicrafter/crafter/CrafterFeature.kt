@file:Suppress("UnstableApiUsage")

package dev.adirelle.adicrafter.crafter

import dev.adirelle.adicrafter.AdiCrafter
import dev.adirelle.adicrafter.crafter.api.power.PowerGenerator
import dev.adirelle.adicrafter.crafter.api.recipe.Recipe
import dev.adirelle.adicrafter.crafter.api.recipe.RecipeFlags
import dev.adirelle.adicrafter.crafter.api.storage.ResourceType
import dev.adirelle.adicrafter.crafter.api.storage.SingleTypeStorageProvider
import dev.adirelle.adicrafter.crafter.api.storage.StorageProvider
import dev.adirelle.adicrafter.crafter.impl.power.IllimitedPowerGenerator
import dev.adirelle.adicrafter.crafter.impl.power.SteadyPowerGenerator
import dev.adirelle.adicrafter.crafter.impl.recipe.FactoryImpl
import dev.adirelle.adicrafter.crafter.impl.storage.NeighborStorageProvider
import dev.adirelle.adicrafter.crafter.impl.storage.StorageCompoundProvider
import dev.adirelle.adicrafter.utils.ModFeature
import net.fabricmc.api.EnvType.CLIENT
import net.fabricmc.api.Environment
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage
import net.minecraft.block.BlockState
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.item.ItemGroup
import net.minecraft.server.world.ServerWorld
import net.minecraft.util.Identifier
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World

object CrafterFeature : ModFeature(AdiCrafter, "crafter") {

    private val config by lazy { AdiCrafter.config.crafter }

    val BLOCK = CrafterBlock(::createCrafterBlockEntity)

    val BLOCK_ENTITY_TYPE = r(ID, BLOCK, ::createCrafterBlockEntity)

    val SCREEN_HANDLER_TYPE = registerExtended(::CrafterScreenHandler)

    override fun onInitialize() {
        ItemStorage.SIDED.registerForBlockEntity(
            { blockEntity, _ -> blockEntity.dataAccessor.crafter },
            BLOCK_ENTITY_TYPE
        )
        LOGGER.info("initializer with {}")
    }

    @Environment(CLIENT)
    override fun onInitializeClient() {
        super.onInitializeClient()
        register(SCREEN_HANDLER_TYPE, ::CrafterScreen)
    }

    private fun <B : CrafterBlock, T : CrafterBlockEntity> r(
        id: Identifier,
        block: B,
        blockEntityFactory: (pos: BlockPos, state: BlockState) -> T
    ): BlockEntityType<T> {
        register(block, id)
        registerItemFor(block, id) { group(ItemGroup.REDSTONE) }
        return register(blockEntityFactory, id, null, block)
    }

    private fun createCrafterBlockEntity(pos: BlockPos, state: BlockState): CrafterBlockEntity {
        val powerGenerator = createPowerGenerator()
        return CrafterBlockEntity(pos, state, powerGenerator, ::createRecipeFactory) { world, _ ->
            createStorageProvider(world, pos, powerGenerator)
        }
    }

    private fun createPowerGenerator() =
        with(config.power) {
            if (enabled) SteadyPowerGenerator(capacity, reloadRate)
            else IllimitedPowerGenerator
        }

    private fun createRecipeFactory(flags: RecipeFlags): Recipe.Factory {
        val actualFlags = flags.set(RecipeFlags.POWER, config.power.enabled)
        return FactoryImpl.with(actualFlags)
    }

    private fun createStorageProvider(world: World?, pos: BlockPos, powerGenerator: PowerGenerator): StorageProvider =
        if (world is ServerWorld)
            StorageCompoundProvider(
                buildMap {
                    put(ResourceType.ITEM, NeighborStorageProvider(ItemStorage.SIDED, world, pos))
                    put(ResourceType.FLUID, NeighborStorageProvider(FluidStorage.SIDED, world, pos))
                    if (powerGenerator.isActive()) {
                        put(ResourceType.POWER, SingleTypeStorageProvider.of(powerGenerator))
                    }
                }
            )
        else
            StorageProvider.EMPTY
}
