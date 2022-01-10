@file:Suppress("UnstableApiUsage")

package dev.adirelle.adicrafter.crafter

import dev.adirelle.adicrafter.crafter.internal.*
import dev.adirelle.adicrafter.utils.Observer
import dev.adirelle.adicrafter.utils.general.ObservableValueHolder
import dev.adirelle.adicrafter.utils.general.extensions.EMPTY_ITEM_AMOUNT
import dev.adirelle.adicrafter.utils.general.lazyLogger
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant
import net.fabricmc.fabric.api.transfer.v1.storage.Storage
import net.fabricmc.fabric.api.transfer.v1.storage.base.ResourceAmount
import net.fabricmc.fabric.api.util.NbtType
import net.minecraft.block.BlockState
import net.minecraft.block.entity.BlockEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.nbt.NbtCompound
import net.minecraft.screen.NamedScreenHandlerFactory
import net.minecraft.text.Text
import net.minecraft.text.TranslatableText
import net.minecraft.util.ItemScatterer
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World

class CrafterBlockEntity(pos: BlockPos, state: BlockState) :
    BlockEntity(Crafter.BLOCK_ENTITY_TYPE, pos, state),
    NamedScreenHandlerFactory {

    companion object {

        private const val CONFIG_NBT_KEY = "Config"
        private const val CONTENT_NBT_KEY = "Content"

        const val GRID_SIZE = Grid.SIZE
        const val GRID_WIDTH = Grid.WIDTH
        const val GRID_HEIGHT = Grid.HEIGHT
    }

    private val logger by lazyLogger

    private var config = CraftingConfig()

    private val inputProvider: InputProvider = object : AbstractInputProvider() {
        override val world by this@CrafterBlockEntity::world
        override val pos by this@CrafterBlockEntity::pos
    }

    private val crafter = RecipeCrafter(config::recipe, inputProvider)
    private val craftingStorage = CraftingStorage(crafter)

    private var dirtyForecast = false
    private val forecastHolder = ObservableValueHolder<ResourceAmount<ItemVariant>>(EMPTY_ITEM_AMOUNT)

    var grid by config::grid
    val recipe by config::recipe
    val content by craftingStorage::content
    val forecast by forecastHolder
    val storage: Storage<ItemVariant> by this::craftingStorage

    init {
        config.observeGrid { markDirty() }
        config.observeRecipe {
            dropContent()
            dirtyForecast = true
        }
        craftingStorage.observeContent {
            dirtyForecast = true
            markDirty()
        }
    }

    fun observeGrid(callback: Observer<Grid>) = config.observeGrid(callback)

    fun observeRecipe(callback: Observer<OptionalRecipe>) = config.observeRecipe(callback)

    fun observeContent(callback: Observer<ResourceAmount<ItemVariant>>) = craftingStorage.observeContent(callback)

    fun observeForecast(callback: Observer<ResourceAmount<ItemVariant>>): AutoCloseable {
        dirtyForecast = true
        return forecastHolder.observeValue(callback)
    }

    fun onNeighborUpdate() {
        logger.info("neighbor updated")
        dirtyForecast = true
    }

    fun tick(world: World) {
        config.cleanRecipe(world)
        updateForecast()
    }

    fun dropContent() {
        pos.up().let { dropPos ->
            ItemScatterer.spawn(
                world,
                dropPos.x.toDouble(),
                dropPos.y.toDouble(),
                dropPos.z.toDouble(),
                craftingStorage.dropItems()
            )
        }
    }

    private fun updateForecast() {
        if (!dirtyForecast) return
        dirtyForecast = false
        forecastHolder.set(craftingStorage.computeForecast())
    }

    override fun getDisplayName(): Text = TranslatableText("block.adicrafter.crafter")

    override fun createMenu(syncId: Int, playerInventory: PlayerInventory, player: PlayerEntity) =
        CrafterScreenHandler(syncId, playerInventory, this)

    override fun readNbt(nbt: NbtCompound) {
        super.readNbt(nbt)

        config.readFromNbt(nbt.getList(CONFIG_NBT_KEY, NbtType.COMPOUND))
        craftingStorage.readFromNbt(nbt.getCompound(CONTENT_NBT_KEY))
    }

    override fun writeNbt(nbt: NbtCompound) {
        super.writeNbt(nbt)

        nbt.put(CONFIG_NBT_KEY, config.toNbt())
        nbt.put(CONTENT_NBT_KEY, craftingStorage.toNbt())
    }
}
