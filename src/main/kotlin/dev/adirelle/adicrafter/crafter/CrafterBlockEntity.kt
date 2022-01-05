package dev.adirelle.adicrafter.crafter

import dev.adirelle.adicrafter.utils.BaseEmitter
import dev.adirelle.adicrafter.utils.Emitter
import dev.adirelle.adicrafter.utils.Receiver
import dev.adirelle.adicrafter.utils.extension.*
import dev.adirelle.adicrafter.utils.lazyLogger
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext
import net.fabricmc.fabric.api.transfer.v1.transaction.base.SnapshotParticipant
import net.minecraft.block.BlockState
import net.minecraft.block.entity.BlockEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.inventory.CraftingInventory
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NbtCompound
import net.minecraft.recipe.CraftingRecipe
import net.minecraft.recipe.RecipeType
import net.minecraft.screen.NamedScreenHandlerFactory
import net.minecraft.screen.ScreenHandler
import net.minecraft.server.world.ServerWorld
import net.minecraft.text.Text
import net.minecraft.text.TranslatableText
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import toVariant
import java.util.*

@Suppress("UnstableApiUsage")
class CrafterBlockEntity(pos: BlockPos, state: BlockState) :
    BlockEntity(Crafter.BLOCK_ENTITY_TYPE, pos, state),
    NamedScreenHandlerFactory {

    companion object Consts {

        private const val GRID_NBT_KEY = "Grid"
        private const val BUFFER_NBT_KEY = "Buffer"

        const val GRID_WIDTH = 3
        const val GRID_HEIGHT = 3
        const val GRID_SIZE = GRID_WIDTH * GRID_HEIGHT
    }

    private val logger by lazyLogger()

    private val craftingGrid = CraftingInventory(
        object : ScreenHandler(null, 0) {
            override fun canUse(player: PlayerEntity) = false
        },
        GRID_WIDTH,
        GRID_HEIGHT
    )

    private var buffer = ItemStack.EMPTY

    private var recipe: Optional<CraftingRecipe> = Optional.empty()
    private val recipeOutput: ItemStack
        get() = recipe.map { it.output }.orElse(ItemStack.EMPTY)

    private var lastOutputSim = ItemStack.EMPTY

    override fun getDisplayName(): Text = TranslatableText("block.adicrafter.crafter")

    override fun createMenu(syncId: Int, playerInventory: PlayerInventory, player: PlayerEntity) =
        CrafterScreenHandler(syncId, playerInventory, this)

    override fun readNbt(nbt: NbtCompound) {
        super.readNbt(nbt)

        val savedBuffer = nbt.getItemStack(BUFFER_NBT_KEY)
        val savedGrid = nbt.getItemStacks(GRID_NBT_KEY).toTypedArray()

        buffer = savedBuffer
        onGridUpdated(savedGrid)
    }

    override fun writeNbt(nbt: NbtCompound) {
        super.writeNbt(nbt)

        nbt.putItemStacks(GRID_NBT_KEY, craftingGrid.toList())
        nbt.putItemStack(BUFFER_NBT_KEY, buffer)
    }

    val gridUpdateEmitter = BaseEmitter<GridUpdate>()

    init {
        gridUpdateEmitter.listen { payload -> onGridUpdated(payload.grid) }
    }

    val recipeUpdateEmitter: Emitter<RecipeUpdate> = object : BaseEmitter<RecipeUpdate>() {
        override fun onNewReceiver(receiver: Receiver<RecipeUpdate>) {
            logger.info("added new recipe receiver {}", receiver)
            receiver.onReceived(RecipeUpdate(craftingGrid.toArray(), recipeOutput))
        }
    }

    val outputUpdateEmitter: Emitter<OutputUpdate> = object : BaseEmitter<OutputUpdate>() {
        override fun onNewReceiver(receiver: Receiver<OutputUpdate>) {
            logger.info("added new output receiver {}", receiver)
            receiver.onReceived(OutputUpdate(buffer, lastOutputSim))
        }
    }

    val storage: SingleSlotStorage<ItemVariant> = StorageImpl()

    private fun onGridUpdated(newGrid: Array<ItemStack>) {
        val oldGrid = toItemString(newGrid)

        for (idx in 0 until GRID_SIZE) {
            val stack = newGrid[idx]
            if (ItemStack.areEqual(stack, craftingGrid.getStack(idx))) {
                continue
            }
            craftingGrid.setStack(idx, stack.copy())
        }
        logger.info("onGridUpdated: {} -> {}", oldGrid, toItemString(craftingGrid))

        resolveRecipe()
        markDirty()
    }

    override fun setWorld(world: World?) {
        logger.info("setWorld: {}", world)
        super.setWorld(world)
        resolveRecipe()
    }

    private fun resolveRecipe() {
        (world as? ServerWorld)?.let { world ->

            val originalId = recipe.map { it.id }

            recipe = Optional.of(craftingGrid)
                .filter { !it.isEmpty }
                .flatMap { world.recipeManager.getFirstMatch(RecipeType.CRAFTING, it, world) }
                .instanceOf()
            logger.info("recipe resolved: {} -> {}", originalId, recipe.map { it.id })

            recipeUpdateEmitter.emit(RecipeUpdate(craftingGrid.toArray(), recipeOutput))
            updateOutputSim()

        }
    }

    private fun updateOutputSim() {
        logger.info("updateOutputSim")

        inOuterTransaction { tx ->
            lastOutputSim = CraftHelper(tx).available().copy()
            logger.info("lastOutputSim: {}", lastOutputSim)
            tx.abort()
        }

        outputUpdateEmitter.emit(OutputUpdate(buffer, lastOutputSim))
    }

    data class GridUpdate(val grid: Array<ItemStack>)

    data class RecipeUpdate(val grid: Array<ItemStack>, val result: ItemStack)

    data class OutputUpdate(val buffer: ItemStack, val output: ItemStack)

    private inner class CraftHelper(val tx: Transaction) : SnapshotParticipant<ItemStack>() {

        fun available(): ItemStack =
            if (buffer.isEmpty) doCraft()
            else buffer

        fun craft(maxAmount: Int): ItemStack {
            updateSnapshots(tx)
            if (buffer.isEmpty) {
                buffer = doCraft()
            }
            return buffer.split(maxAmount)
        }

        override fun onFinalCommit() {
            logger.info("updating output simulation after crafting")
            updateOutputSim()
        }

        private fun doCraft(): ItemStack = recipe.map { it.output }.orElse(ItemStack.EMPTY).copy()

        override fun createSnapshot(): ItemStack = buffer.copy()
        override fun readSnapshot(snapshot: ItemStack) {
            buffer = snapshot
        }
    }

    private inner class StorageImpl : SingleSlotStorage<ItemVariant> {

        override fun supportsInsertion() = false
        override fun insert(resource: ItemVariant, maxAmount: Long, txc: TransactionContext?) = 0L

        override fun extract(resource: ItemVariant, maxAmount: Long, txc: TransactionContext?) =
            if (resource.isBlank || maxAmount < 1)
                0L
            else
                inNestedTransaction(txc) { tx ->
                    val helper = CraftHelper(tx)
                    var extracted = 0L
                    var crafted = helper.craft(maxAmount.toInt())
                    while (extracted < maxAmount && resource.matches(crafted) && crafted.count > 0) {
                        extracted += crafted.count.toLong()
                        crafted = helper.craft((maxAmount - extracted).toInt())
                    }
                    extracted
                }

        override fun isResourceBlank() = lastOutputSim.isEmpty
        override fun getResource() = lastOutputSim.toVariant()
        override fun getAmount() = lastOutputSim.count.toLong()
        override fun getCapacity() = lastOutputSim.maxCount.toLong()
    }
}
