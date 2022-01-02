package dev.adirelle.adicrafter.blockentity

import dev.adirelle.adicrafter.AdiCrafter.CRAFTER_BLOCK_ENTITY
import dev.adirelle.adicrafter.screen.CrafterScreenHandler
import dev.adirelle.adicrafter.utils.extension.putIdentifier
import dev.adirelle.adicrafter.utils.lazyLogger
import dev.adirelle.adicrafter.utils.onChangeCallback
import net.fabricmc.fabric.api.util.NbtType
import net.minecraft.block.BlockState
import net.minecraft.block.entity.BlockEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NbtCompound
import net.minecraft.nbt.NbtList
import net.minecraft.screen.NamedScreenHandlerFactory
import net.minecraft.text.Text
import net.minecraft.text.TranslatableText
import net.minecraft.util.Identifier
import net.minecraft.util.math.BlockPos
import net.minecraft.util.registry.Registry

@Suppress("UnstableApiUsage")
class CrafterBlockEntity(pos: BlockPos, state: BlockState) :
    BlockEntity(CRAFTER_BLOCK_ENTITY, pos, state),
    NamedScreenHandlerFactory {

    companion object {

        private const val GRID_NBT_KEY = "Grid"
        private const val RESULT_NBT_KEY = "Result"
        private const val RECIPE_NBT_KEY = "Recipe"
        private const val BUFFER_NBT_KEY = "Buffer"
        private const val OUTPUT_NBT_KEY = "Output"
    }

    private val logger by lazyLogger()

    var config by onChangeCallback(Config.EMPTY, this::onConfigChanged)
    var buffer by onChangeCallback(ItemStack.EMPTY, this::markDirty)
    var output by onChangeCallback(ItemStack.EMPTY, this::markDirty)

    private fun onConfigChanged() {
        logger.info("new config: {}", config)
        markDirty()
    }

    override fun createMenu(syncId: Int, playerInventory: PlayerInventory, player: PlayerEntity) =
        CrafterScreenHandler(syncId, playerInventory, this)

    override fun getDisplayName(): Text =
        TranslatableText("block.adicrafter.crafter")

    override fun readNbt(nbt: NbtCompound) {
        super.readNbt(nbt)
        config = Config.fromNbt(nbt)
        buffer = ItemStack.fromNbt(nbt.getCompound(BUFFER_NBT_KEY))
        output = ItemStack.fromNbt(nbt.getCompound(OUTPUT_NBT_KEY))
        logger.info("read from NBT: {}, {}, {}", config, buffer, output)
    }

    override fun writeNbt(nbt: NbtCompound) {
        super.writeNbt(nbt)
        config.writeNbt(nbt)
        nbt.put(BUFFER_NBT_KEY, NbtCompound().apply { buffer.writeNbt(this) })
        nbt.put(OUTPUT_NBT_KEY, NbtCompound().apply { output.writeNbt(this) })
        logger.info("writting to NBT: {}, {}, {}", config, buffer, output)
    }

//    @Environment(SERVER)
//    private fun updateRecipe() {
//        logger.info("updating recipe, grid: {}, isClient? {}", grid, world?.isClient)
//        val world = this.world ?: return
//
//        val craftingGrid by lazy {
//            val dummy = object : ScreenHandler(null, 0) {
//                override fun canUse(player: PlayerEntity) = false
//            }
//            CraftingInventory(dummy, 3, 3)
//        }
//
//        craftingGrid.clear()
//        for (slot in 0 until craftingGrid.size()) {
//            craftingGrid.setStack(slot, grid.getStack(slot))
//        }
//
//        world.recipeManager
//            .getFirstMatch(RecipeType.CRAFTING, craftingGrid, world)
//            .ifPresentOrElse(
//                { recipe ->
//                    logger.info("found recipe: {}", recipe.id)
//                    result.stack = recipe.output.copy()
//                }, {
//                    logger.info("no recipe found")
//                    result.stack = EMPTY
//                }
//            )
//    }

    data class Config(
        val recipeId: Identifier,
        val grid: Array<ItemStack>,
        val result: ItemStack,
    ) {

        companion object {

            private val logger by lazyLogger()

            val EMPTY_ID = Registry.ITEM.defaultId
            val EMPTY = Config(EMPTY_ID, Array(9) { ItemStack.EMPTY }, ItemStack.EMPTY)

            fun fromNbt(nbt: NbtCompound) =
                try {
                    val recipeId = Identifier(nbt.getString(RECIPE_NBT_KEY))
                    val grid = nbt.getList(GRID_NBT_KEY, NbtType.COMPOUND)
                        .filterIsInstance(NbtCompound::class.java)
                        .map { ItemStack.fromNbt(it) }
                        .subList(0, 9)
                        .toTypedArray()
                    val result = ItemStack.fromNbt(nbt.getCompound(RESULT_NBT_KEY))
                    Config(recipeId, grid, result)
                } catch (e: Exception) {
                    logger.warn("could not read config from NBT")
                    EMPTY
                }
        }

        fun writeNbt(nbt: NbtCompound): NbtCompound {
            nbt.putIdentifier(RECIPE_NBT_KEY, recipeId)
            nbt.put(GRID_NBT_KEY, NbtList().also { list ->
                grid.forEach { item ->
                    list.add(NbtCompound().also { item.writeNbt(it) })
                }
            })
            nbt.put(RESULT_NBT_KEY, NbtCompound().also { result.writeNbt(it) })
            return nbt
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Config

            if (recipeId != other.recipeId) return false
            if (!grid.contentEquals(other.grid)) return false
            if (result != other.result) return false

            return true
        }

        override fun hashCode(): Int {
            var result1 = recipeId.hashCode()
            result1 = 31 * result1 + grid.hashCode()
            result1 = 31 * result1 + result.hashCode()
            return result1
        }
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
