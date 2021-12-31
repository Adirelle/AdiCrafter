package dev.adirelle.adicrafter.blockentity

import dev.adirelle.adicrafter.AdiCrafter.CRAFTER_BLOCK_ENTITY
import dev.adirelle.adicrafter.screen.CrafterScreenHandler
import dev.adirelle.adicrafter.utils.InventoryView
import dev.adirelle.adicrafter.utils.readNbt
import dev.adirelle.adicrafter.utils.toNbt
import net.fabricmc.fabric.api.util.NbtType
import net.minecraft.block.BlockState
import net.minecraft.block.entity.BlockEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NbtCompound
import net.minecraft.screen.NamedScreenHandlerFactory
import net.minecraft.text.Text
import net.minecraft.text.TranslatableText
import net.minecraft.util.Identifier
import net.minecraft.util.collection.DefaultedList
import net.minecraft.util.math.BlockPos
import org.apache.logging.log4j.LogManager
import java.util.*


class CrafterBlockEntity(pos: BlockPos, state: BlockState) :
    BlockEntity(CRAFTER_BLOCK_ENTITY, pos, state),
    NamedScreenHandlerFactory {

    companion object {
        private val LOGGER = LogManager.getLogger()

        const val GRID_SIZE = 9
        const val RESULT_SLOT = GRID_SIZE
        const val BUFFER_SLOT = RESULT_SLOT + 1
        const val OUTPUT_SLOT = BUFFER_SLOT + 1
        const val INVENTORY_SIZE = OUTPUT_SLOT + 1

        private const val INVENTORY_NBT_KEY = "Inventory"
        private const val RECIPE_NBT_KEY = "Recipe"
    }

    private val stacks = DefaultedList.ofSize(INVENTORY_SIZE, ItemStack.EMPTY)

    val grid = object : InventoryView(stacks.subList(0, BUFFER_SLOT)) {
        override fun markDirty() {
            doMarkDirty("grid")
        }
    }

    val buffer = object : InventoryView(stacks.subList(BUFFER_SLOT, OUTPUT_SLOT)) {
        override fun markDirty() {
            doMarkDirty("buffer")
        }
    }

    val result = object : InventoryView(stacks.subList(RESULT_SLOT, OUTPUT_SLOT)) {
        override fun markDirty() {
            doMarkDirty("result")
        }
    }

    val output = object : InventoryView(stacks.subList(OUTPUT_SLOT, INVENTORY_SIZE)) {
        override fun markDirty() {
            doMarkDirty("output")
        }
    }

    private fun doMarkDirty(reason: String = "because") {
        LOGGER.info("marked dirty {}", reason)
        markDirty()
    }

    var recipeIdentifier: Optional<Identifier> = Optional.empty()
        set(value) {
            if (field != value) {
                field = value
                doMarkDirty("recipe")
            }
        }

    override fun createMenu(syncId: Int, playerInventory: PlayerInventory, player: PlayerEntity) =
        CrafterScreenHandler(syncId, playerInventory, this)

    override fun getDisplayName(): Text =
        TranslatableText("block.adicrafter.crafter")

    override fun readNbt(nbt: NbtCompound) {
        super.readNbt(nbt)
        recipeIdentifier = Optional.ofNullable(Identifier.tryParse(nbt.getString(RECIPE_NBT_KEY)))
        stacks.readNbt(nbt.getList(INVENTORY_NBT_KEY, NbtType.COMPOUND))
        LOGGER.info("read from NBT: {}, {}", recipeIdentifier, stacks)
    }

    override fun writeNbt(nbt: NbtCompound) {
        super.writeNbt(nbt)
        nbt.putString(RECIPE_NBT_KEY, recipeIdentifier.map(Identifier::toString).orElse(""))
        nbt.put(INVENTORY_NBT_KEY, stacks.toNbt())
        LOGGER.info("written in NBT: {}, {}", recipeIdentifier, stacks)
    }
}
