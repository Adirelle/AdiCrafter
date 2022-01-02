package dev.adirelle.adicrafter.blockentity

import dev.adirelle.adicrafter.AdiCrafter.CRAFTER_BLOCK_ENTITY
import dev.adirelle.adicrafter.screen.CrafterScreenHandler
import dev.adirelle.adicrafter.utils.inventory.api.StackDisplayInventory
import dev.adirelle.adicrafter.utils.lazyLogger
import dev.adirelle.adicrafter.utils.storage.SimpleInventoryStorage
import net.fabricmc.api.EnvType.SERVER
import net.fabricmc.api.Environment
import net.fabricmc.fabric.api.util.NbtType
import net.minecraft.block.BlockState
import net.minecraft.block.entity.BlockEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.inventory.CraftingInventory
import net.minecraft.item.ItemStack.EMPTY
import net.minecraft.nbt.NbtCompound
import net.minecraft.nbt.NbtList
import net.minecraft.recipe.RecipeType
import net.minecraft.screen.NamedScreenHandlerFactory
import net.minecraft.screen.ScreenHandler
import net.minecraft.text.Text
import net.minecraft.text.TranslatableText
import net.minecraft.util.math.BlockPos

@Suppress("UnstableApiUsage")
class CrafterBlockEntity(pos: BlockPos, state: BlockState) :
    BlockEntity(CRAFTER_BLOCK_ENTITY, pos, state),
    NamedScreenHandlerFactory {

    companion object {

        private const val GRID_NBT_KEY = "Grid"
        private const val BUFFER_NBT_KEY = "Buffer"
    }

    private val logger by lazyLogger()

    private val grid = SimpleInventoryStorage(9)
    private val buffer = SimpleInventoryStorage(1)
    private val result = StackDisplayInventory.of(EMPTY)

    init {
        if (world?.isClient == false) {
            grid.onContentChanged { updateRecipe() }
        }
        grid.onContentChanged { markDirty() }
        buffer.onContentChanged { markDirty() }
    }

    override fun createMenu(syncId: Int, playerInventory: PlayerInventory, player: PlayerEntity) =
        CrafterScreenHandler(syncId, playerInventory, grid, buffer, result)

    override fun getDisplayName(): Text =
        TranslatableText("block.adicrafter.crafter")

    override fun readNbt(nbt: NbtCompound) {
        super.readNbt(nbt)
        grid.readNbt(nbt.getList(GRID_NBT_KEY, NbtType.COMPOUND))
        buffer.readNbt(nbt.getList(BUFFER_NBT_KEY, NbtType.COMPOUND))
        logger.info("read from NBT: {}, {}", buffer, grid)
    }

    override fun writeNbt(nbt: NbtCompound) {
        super.writeNbt(nbt)
        nbt.put(GRID_NBT_KEY, grid.writeNbt(NbtList()))
        nbt.put(BUFFER_NBT_KEY, buffer.writeNbt(NbtList()))
        logger.info("written in NBT: {}, {}, {}", buffer, grid)
    }

    @Environment(SERVER)
    private fun updateRecipe() {
        logger.info("updating recipe, grid: {}, isClient? {}", grid, world?.isClient)
        val world = this.world ?: return

        val craftingGrid by lazy {
            val dummy = object : ScreenHandler(null, 0) {
                override fun canUse(player: PlayerEntity) = false
            }
            CraftingInventory(dummy, 3, 3)
        }

        craftingGrid.clear()
        for (slot in 0 until craftingGrid.size()) {
            craftingGrid.setStack(slot, grid.getStack(slot))
        }

        world.recipeManager
            .getFirstMatch(RecipeType.CRAFTING, craftingGrid, world)
            .ifPresentOrElse(
                { recipe ->
                    logger.info("found recipe: {}", recipe.id)
                    result.stack = recipe.output.copy()
                }, {
                    logger.info("no recipe found")
                    result.stack = EMPTY
                }
            )
    }

/*
private class BufferedStorage(
    val buffer: Storage<ItemVariant>,
    val upstream: Storage<ItemVariant>
) : SingleVariantStorage<ItemVariant>() {

    override fun getCapacity(variant: ItemVariant) =
        variant.item.maxCount.toLong()

    override fun getBlankVariant(): ItemVariant =
        ItemVariant.blank()

    override fun extract(resource: ItemVariant, maxAmount: Long, tx: TransactionContext?): Long {
        var extracted = buffer.extract(resource, maxAmount, tx)
        if (extracted < maxAmount) {
            extracted += upstream.extract(resource, maxAmount - extracted, tx)
        }
        if (extracted > maxAmount) {
            extracted -= buffer.insert(resource, extracted - maxAmount, tx)
        }
        return extracted
    }

    override fun insert(resource: ItemVariant, maxAmount: Long, tx: TransactionContext?) =
        buffer.insert(resource, maxAmount, tx)
}

private inner class CraftingStorage : SingleVariantStorage<ItemVariant>() {

    override fun extract(resource: ItemVariant, maxAmount: Long, transaction: TransactionContext?): Long {
        if (resource != this.resource || maxAmount < 1) return 0
        var crafted = 0L
        while (crafted < maxAmount) {
            // TODO: search and consume ingredients
            crafted += amount
        }
        return crafted
    }

    override fun getCapacity(variant: ItemVariant) =
        result.maxCountPerStack.toLong()

    override fun getBlankVariant(): ItemVariant =
        ItemVariant.blank()

    override fun getResource(): ItemVariant =
        if (result.isEmpty) blankVariant else ItemVariant.of(result.getStack(0))

    override fun getAmount() =
        result.getStack(0).count.toLong()

    override fun supportsInsertion() =
        false

    override fun insert(variant: ItemVariant?, maxAmount: Long, transaction: TransactionContext?) =
        0L
}
*/
}
