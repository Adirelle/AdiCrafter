@file:Suppress("UnstableApiUsage")

package dev.adirelle.adicrafter.crafter

import dev.adirelle.adicrafter.AdiCrafter
import dev.adirelle.adicrafter.crafter.api.power.PowerGenerator
import dev.adirelle.adicrafter.crafter.api.recipe.Recipe
import dev.adirelle.adicrafter.crafter.api.recipe.RecipeFlags
import dev.adirelle.adicrafter.crafter.api.storage.ResourceType
import dev.adirelle.adicrafter.crafter.api.storage.SingleTypeStorageProvider
import dev.adirelle.adicrafter.crafter.api.storage.StorageProvider
import dev.adirelle.adicrafter.crafter.impl.power.DefaultGenerator
import dev.adirelle.adicrafter.crafter.impl.power.IllimitedGenerator
import dev.adirelle.adicrafter.crafter.impl.power.SolidFuelGenerator
import dev.adirelle.adicrafter.crafter.impl.recipe.FactoryImpl
import dev.adirelle.adicrafter.crafter.impl.storage.NeighborStorageProvider
import dev.adirelle.adicrafter.crafter.impl.storage.StorageCompoundProvider
import dev.adirelle.adicrafter.utils.ModFeature
import dev.adirelle.adicrafter.utils.storage.SingleViewStorage
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

@Suppress("MemberVisibilityCanBePrivate")
object CrafterFeature : ModFeature(AdiCrafter, "crafter") {

    private val config by lazy { AdiCrafter.config.crafter }

    val BASIC_CRAFTER = CrafterBlock(::createBasicCrafterBlockEntity)
    val FUELED_CRAFTER = CrafterBlock(::createFueledCrafterBlockEntity)

    val BASIC_CRAFTER_ENTITY_TYPE: BlockEntityType<CrafterBlockEntity> =
        r(ID, BASIC_CRAFTER, ::createBasicCrafterBlockEntity)

    val FUELED_CRAFTER_ENTITY_TYPE: BlockEntityType<CrafterBlockEntity> =
        r(id("fueled_crafter"), FUELED_CRAFTER, ::createFueledCrafterBlockEntity)

    val SCREEN_HANDLER_TYPE = registerExtended(::CrafterScreenHandler)

    @Environment(CLIENT)
    override fun onInitializeClient() {
        super.onInitializeClient()
        register(SCREEN_HANDLER_TYPE, ::CrafterScreen)
    }

    private fun r(
        id: Identifier,
        block: CrafterBlock,
        blockEntityFactory: (pos: BlockPos, state: BlockState) -> CrafterBlockEntity
    ): BlockEntityType<CrafterBlockEntity> {
        register(block, id)
        registerItemFor(block, id) { group(ItemGroup.REDSTONE) }
        return register(blockEntityFactory, id, null, block).also {
            ItemStorage.SIDED.registerForBlockEntity({ blockEntity, _ -> blockEntity.dataAccessor.crafter }, it)
        }
    }

    private fun createBasicCrafterBlockEntity(pos: BlockPos, state: BlockState) =
        createCrafterBlockEntity(BASIC_CRAFTER_ENTITY_TYPE, pos, state, createBasicPowerGenerator())

    private fun createBasicPowerGenerator() =
        if (config.usePower) DefaultGenerator(config.basic.capacity, config.basic.reloadRate)
        else IllimitedGenerator

    private fun createFueledCrafterBlockEntity(pos: BlockPos, state: BlockState) =
        createCrafterBlockEntity(
            FUELED_CRAFTER_ENTITY_TYPE,
            pos,
            state,
            SolidFuelGenerator(config.solidFuel.capacity, config.solidFuel.reloadRate)
        )

    private fun createCrafterBlockEntity(
        blockEntityType: BlockEntityType<CrafterBlockEntity>,
        pos: BlockPos,
        state: BlockState,
        powerGenerator: PowerGenerator
    ) =
        CrafterBlockEntity(blockEntityType, pos, state, powerGenerator, ::createRecipeFactory) { world, _ ->
            createStorageProvider(world, pos, powerGenerator)
        }

    private fun createRecipeFactory(flags: RecipeFlags): Recipe.Factory {
        val actualFlags = flags.set(RecipeFlags.POWER, config.usePower)
        return FactoryImpl.with(actualFlags)
    }

    private fun createStorageProvider(world: World?, pos: BlockPos, powerGenerator: PowerGenerator): StorageProvider =
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
