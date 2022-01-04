package dev.adirelle.adicrafter.crafter

import dev.adirelle.adicrafter.utils.BoxedProperty
import dev.adirelle.adicrafter.utils.extension.logger
import dev.adirelle.adicrafter.utils.extension.toItemString
import dev.adirelle.adicrafter.utils.lazyLogger
import net.fabricmc.fabric.api.util.NbtType
import net.minecraft.block.BlockState
import net.minecraft.block.entity.BlockEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NbtCompound
import net.minecraft.nbt.NbtList
import net.minecraft.recipe.CraftingRecipe
import net.minecraft.screen.NamedScreenHandlerFactory
import net.minecraft.screen.ScreenHandler
import net.minecraft.text.Text
import net.minecraft.text.TranslatableText
import net.minecraft.util.Identifier
import net.minecraft.util.math.BlockPos
import java.util.*

@Suppress("UnstableApiUsage")
class CrafterBlockEntity(pos: BlockPos, state: BlockState) :
    BlockEntity(Crafter.BLOCK_ENTITY_TYPE, pos, state),
    NamedScreenHandlerFactory {

    companion object {

        private const val RECIPE_NBT_KEY = "Recipe"
        private const val INVENTORY_NBT_KEY = "Inventory"

        private const val GRID_SIZE = 9
        private const val RESULT_SLOT = GRID_SIZE
        private const val BUFFER_SLOT = RESULT_SLOT + 1
        private const val INVENTORY_SIZE = BUFFER_SLOT + 1
    }

    private val logger by lazyLogger()

    private val stacks = MutableList<ItemStack>(INVENTORY_SIZE) { ItemStack.EMPTY }
    private val grid: MutableList<ItemStack> = stacks.subList(0, GRID_SIZE)
    private var recipe: Identifier? = null

    private var recipeOutput = BoxedProperty(
        { stacks[RESULT_SLOT] },
        { v -> stacks[RESULT_SLOT] = v.copy() },
        ItemStack::areEqual
    )

    private var buffer = BoxedProperty(
        { stacks[BUFFER_SLOT] },
        { v -> stacks[BUFFER_SLOT] = v.copy() },
        ItemStack::areEqual
    )

    private var listeners = ListenerList()

    override fun createMenu(syncId: Int, playerInventory: PlayerInventory, player: PlayerEntity): ScreenHandler {
        val newScreenHandler = CrafterScreenHandler(syncId, playerInventory, this)
        listeners.add(newScreenHandler)
        updateListenerRecipe(newScreenHandler)
        updateListenerOutput(newScreenHandler)
        return newScreenHandler
    }

    fun onListenerClosed(listener: Listener) {
        listeners.remove(listener)
    }

    private fun updateListenerRecipe(listener: Listener = listeners) {
        listener.onRecipeChanged(stacks.subList(0, GRID_SIZE), stacks[RESULT_SLOT])
    }

    private fun updateListenerOutput(listener: Listener = listeners) {
    }

    fun setRecipe(recipe: CraftingRecipe?, grid: Array<ItemStack>) {
        logger.info("updateRecipeFromScreen: {} {}", recipe, toItemString(grid))

        this.recipe = recipe?.id
        val x: ItemStack?
        this.grid.indices.forEach {
            this.grid[i] = grid[i].copy()
        }
        for (i in 0 until GRID_SIZE) {
            stacks[i] = grid[i].copy()
        }
        recipeOutput = recipe?.output?.copy() ?: ItemStack.EMPTY
    }

    private var isSimDirty = true

    private fun updateOutputSim() {
        if (listeners.isEmpty() || !isSimDirty) return

//
//        transactional(txc) { tx ->
//            val extractable = storage.extract(ItemVariant.blank(), 1, tx)
//            if (extractable > 0) {
//            } else {
//
//            }
//        }
    }

    override fun getDisplayName(): Text = TranslatableText("block.adicrafter.crafter")

    override fun readNbt(nbt: NbtCompound) {
        super.readNbt(nbt)
        recipe = Identifier.tryParse(nbt.getString(RECIPE_NBT_KEY))
        val itemList = nbt.getList(INVENTORY_NBT_KEY, NbtType.COMPOUND)
        for (i in 0 until INVENTORY_SIZE) {
            stacks[i] = (itemList[i] as? NbtCompound)?.let {
                ItemStack.fromNbt(it)
            } ?: ItemStack.EMPTY
        }
        updateListenerRecipe()
    }

    override fun writeNbt(nbt: NbtCompound) {
        super.writeNbt(nbt)
        nbt.putString(RECIPE_NBT_KEY, recipe.toString())
        nbt.put(INVENTORY_NBT_KEY, NbtList().apply {
            for (i in 0 until INVENTORY_SIZE) {
                add(NbtCompound().also {
                    stacks[i].writeNbt(it)
                })
            }
        })
    }

    interface Listener {

        fun onRecipeChanged(gridStacks: List<ItemStack>, resultStack: ItemStack)
        fun onOutputChanged(bufferStack: ItemStack, outputStack: ItemStack)
    }

    private class ListenerList : Listener {

        private val listeners: MutableList<Listener> = Collections.synchronizedList(ArrayList(2))

        fun isEmpty() = listeners.isEmpty()

        override fun onRecipeChanged(gridStacks: List<ItemStack>, resultStack: ItemStack) {
            logger.info("updating listener recipes: {}, {}", toItemString(gridStacks), resultStack)
            synchronized(listeners) {
                listeners.forEach { it.onRecipeChanged(gridStacks, resultStack) }
            }
        }

        override fun onOutputChanged(bufferStack: ItemStack, outputStack: ItemStack) {
            logger.info("updating listener output: {}, {}", toItemString(bufferStack), outputStack)
            synchronized(listeners) {
                listeners.forEach { it.onOutputChanged(bufferStack, outputStack) }
            }
        }

        fun add(listener: Listener) {
            logger.info("adding listener: {}", listener)
            listeners.add(listener)
        }

        fun remove(listener: Listener) {
            logger.info("removing listener: {}", listener)
            listeners.remove(listener)
        }
    }

    //    private class CraftingStorage(txc: TransactionContext?) : StorageView<ItemVariant> {
//
//        lateinit var result: ResourceAmount<ItemVariant>
//
//        init {
//
//        }
//
//        override fun extract(resource: ItemVariant?, maxAmount: Long, transaction: TransactionContext?): Long {
//            TODO("Not yet implemented")
//        }
//
//        override fun isResourceBlank() = result.resource.isBlank
//        override fun getResource() = result.resource
//        override fun getAmount() = result.amount
//        override fun getCapacity() = result.resource.TODO("Not yet implemented")
//    }
//}
//
//private inner class StorageImpl : Storage<ItemVariant>,
//                                  StorageView<ItemVariant>,
//                                  SnapshotParticipant<ResourceAmount<ItemVariant>>() {
//
//    override fun supportsInsertion() = false
//    override fun insert(resource: ItemVariant, maxAmount: Long, txc: TransactionContext?) = {
//        if (buffer.)
//    }
//    0L
//
//    override fun extract(resource: ItemVariant, maxAmount: Long, txc: TransactionContext?): Long {
//        TODO("Not yet implemented")
//    }
//
//    override fun iterator(transaction: TransactionContext?): MutableIterator<StorageView<ItemVariant>> =
//        object : MutableIterator<StorageView<ItemVariant>> {
//            override fun next(): StorageView<ItemVariant> = this@StorageImpl
//            override fun hasNext() = false
//            override fun remove() {}
//        }
//
//    override fun isResourceBlank() = extractable.isEmpty()
//    override fun getResource(): ItemVariant = extractable.resource
//    override fun getAmount() = extractable.amount
//    override fun getCapacity() =
//        if (extractable.isEmpty()) Item.DEFAULT_MAX_COUNT.toLong() else extractable.maxStackAmount()
//
//    override fun createSnapshot() = buffer
//
//    override fun readSnapshot(snapshot: ResourceAmount<ItemVariant>) {
//        buffer = snapshot
//    }
//}

}
