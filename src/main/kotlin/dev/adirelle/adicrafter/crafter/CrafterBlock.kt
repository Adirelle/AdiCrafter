@file:Suppress("UnstableApiUsage")

package dev.adirelle.adicrafter.crafter

import net.fabricmc.fabric.api.`object`.builder.v1.block.FabricBlockSettings
import net.fabricmc.fabric.api.block.BlockAttackInteractionAware
import net.minecraft.block.BlockRenderType
import net.minecraft.block.BlockState
import net.minecraft.block.BlockWithEntity
import net.minecraft.block.Material
import net.minecraft.block.entity.BlockEntity
import net.minecraft.block.entity.BlockEntityTicker
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.entity.ItemEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ItemStack
import net.minecraft.loot.context.LootContext.Builder
import net.minecraft.loot.context.LootContextParameters
import net.minecraft.util.ActionResult
import net.minecraft.util.Hand
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.util.math.Vec3d
import net.minecraft.world.World

open class CrafterBlock(
    private val blockEntityFactory: (pos: BlockPos, state: BlockState) -> CrafterBlockEntity
) : BlockWithEntity(
    FabricBlockSettings
        .of(Material.METAL)
        .strength(4.0f)
), BlockAttackInteractionAware {

    override fun createBlockEntity(pos: BlockPos, state: BlockState): BlockEntity =
        blockEntityFactory(pos, state)

    override fun <T : BlockEntity> getTicker(
        world: World,
        state: BlockState,
        type: BlockEntityType<T>
    ): BlockEntityTicker<T>? =
        if (!world.isClient)
            BlockEntityTicker { wrld, _, _, blockEntity -> (blockEntity as? CrafterBlockEntity)?.tick(wrld) }
        else
            null

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

    override fun getDroppedStacks(state: BlockState, builder: Builder): MutableList<ItemStack> =
        (builder.getNullable(LootContextParameters.BLOCK_ENTITY) as? CrafterBlockEntity)
            ?.getDroppedStacks()
            ?.toMutableList()
            ?: super.getDroppedStacks(state, builder)

    override fun onAttackInteraction(
        state: BlockState,
        world: World,
        pos: BlockPos,
        player: PlayerEntity,
        hand: Hand,
        direction: Direction
    ): Boolean {
        if (world.isClient ||
            player.isSpectator || !player.abilities.allowModifyWorld || !player.getStackInHand(hand).isEmpty
        ) {
            return false
        }
        val blockEntity = world.getBlockEntity(pos) as? CrafterBlockEntity ?: return false

        val stack = blockEntity.dataAccessor.craft()
        val unit = Vec3d(direction.unitVector)
        val vpos = Vec3d.ofCenter(pos, 0.5).add(unit.multiply(0.5))
        val vel = vpos.relativize(player.eyePos).normalize()
        val itemEntity = ItemEntity(world, vpos.x, vpos.y, vpos.z, stack, vel.x, vel.y, vel.z)
        world.spawnEntity(itemEntity)

        return true
    }

    override fun getRenderType(state: BlockState?) = BlockRenderType.MODEL
}

