package dev.adirelle.adicrafter.crafter

import net.fabricmc.fabric.api.`object`.builder.v1.block.FabricBlockSettings
import net.minecraft.block.BlockRenderType
import net.minecraft.block.BlockState
import net.minecraft.block.BlockWithEntity
import net.minecraft.block.Material
import net.minecraft.block.entity.BlockEntity
import net.minecraft.block.entity.BlockEntityTicker
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.server.world.ServerWorld
import net.minecraft.util.ActionResult
import net.minecraft.util.Hand
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World

open class CrafterBlock(
    private val blockEntityFactory: (pos: BlockPos, state: BlockState) -> CrafterBlockEntity
) : BlockWithEntity(
    FabricBlockSettings
        .of(Material.METAL)
        .strength(4.0f)
) {

    override fun createBlockEntity(pos: BlockPos, state: BlockState): BlockEntity =
        blockEntityFactory(pos, state)

    override fun <T : BlockEntity> getTicker(
        world: World,
        state: BlockState,
        type: BlockEntityType<T>
    ): BlockEntityTicker<T>? =
        (world as? ServerWorld)?.let { wrld ->
            when (type) {
                CrafterFeature.BASIC_CRAFTER_ENTITY_TYPE,
                CrafterFeature.FUELED_CRAFTER_ENTITY_TYPE,
                CrafterFeature.REDSTONE_CRAFTER_ENTITY_TYPE ->
                    BlockEntityTicker { _, _, _, be ->
                        (be as? CrafterBlockEntity)?.tick(wrld)
                    }
                else                                        ->
                    null
            }
        }

    override fun onStateReplaced(
        state: BlockState,
        world: World,
        pos: BlockPos,
        newState: BlockState,
        moved: Boolean
    ) {
        if (!state.isOf(newState.block) && !world.isClient) {
            (world.getBlockEntity(pos) as? CrafterBlockEntity)
                ?.dropContent()
        }
        @Suppress("DEPRECATION")
        super.onStateReplaced(state, world, pos, newState, moved)
    }

    override fun onUse(
        state: BlockState,
        world: World,
        pos: BlockPos,
        player: PlayerEntity,
        hand: Hand,
        hit: BlockHitResult
    ): ActionResult {
        if (!world.isClient) {
            state.createScreenHandlerFactory(world, pos)?.let { player.openHandledScreen(it) }
        }
        return ActionResult.SUCCESS
    }

    override fun getRenderType(state: BlockState?) = BlockRenderType.MODEL
}
