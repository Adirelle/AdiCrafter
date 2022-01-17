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

class CrafterBlock : BlockWithEntity(
    FabricBlockSettings
        .of(Material.METAL)
        .strength(4.0f)
) {

    override fun createBlockEntity(pos: BlockPos, state: BlockState): BlockEntity =
        CrafterBlockEntity(pos, state)

    override fun <T : BlockEntity> getTicker(
        world: World,
        state: BlockState,
        type: BlockEntityType<T>
    ): BlockEntityTicker<T>? {
        return (world as? ServerWorld)?.let { wrld ->
            checkType(type, CrafterFeature.BLOCK_ENTITY_TYPE) { _, _, _, blockEntity ->
                blockEntity.tick(wrld)
            }
        }
    }

    private fun getBlockEntity(world: World, pos: BlockPos): CrafterBlockEntity? =
        (world as? ServerWorld)?.let { (world.getBlockEntity(pos) as? CrafterBlockEntity) }

    override fun onStateReplaced(
        state: BlockState,
        world: World,
        pos: BlockPos,
        newState: BlockState,
        moved: Boolean
    ) {
        if (!state.isOf(newState.block)) {
            getBlockEntity(world, pos)?.dropContent()
            @Suppress("DEPRECATION")
            super.onStateReplaced(state, world, pos, newState, moved)
        }
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
