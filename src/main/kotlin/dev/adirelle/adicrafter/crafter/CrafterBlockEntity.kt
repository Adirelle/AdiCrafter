@file:Suppress("UnstableApiUsage")

package dev.adirelle.adicrafter.crafter

import dev.adirelle.adicrafter.crafter.internal.CraftingConfig
import dev.adirelle.adicrafter.crafter.internal.CraftingStorage
import dev.adirelle.adicrafter.crafter.internal.Grid
import dev.adirelle.adicrafter.crafter.internal.StandardIngredientExtractor
import dev.adirelle.adicrafter.utils.general.ObservableValueHolder
import dev.adirelle.adicrafter.utils.general.Observer
import dev.adirelle.adicrafter.utils.general.lazyLogger
import dev.adirelle.adicrafter.utils.minecraft.extensions.EMPTY_ITEM_AMOUNT
import dev.adirelle.adicrafter.utils.minecraft.extensions.toAmount
import dev.adirelle.adicrafter.utils.minecraft.withOuterTransaction
import net.fabricmc.fabric.api.lookup.v1.block.BlockApiCache
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant
import net.fabricmc.fabric.api.transfer.v1.storage.Storage
import net.fabricmc.fabric.api.transfer.v1.storage.base.ResourceAmount
import net.minecraft.block.BlockState
import net.minecraft.block.entity.BlockEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.nbt.NbtCompound
import net.minecraft.screen.NamedScreenHandlerFactory
import net.minecraft.server.world.ServerWorld
import net.minecraft.text.Text
import net.minecraft.text.TranslatableText
import net.minecraft.util.ItemScatterer
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
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

    private val extractor = StandardIngredientExtractor(this::findStorages)
    private val crafter = RecipeCrafter(config::recipe, extractor)

    private val craftingStorage = CraftingStorage(crafter)

    private var updateDelay = 0
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
            markForeCastDirty()
        }
        craftingStorage.observeContent {
            markDirty()
        }
    }

    private fun markForeCastDirty() {
        updateDelay = 0
    }

    override fun markDirty() {
        markForeCastDirty()
        super.markDirty()
    }

    fun observeGrid(callback: Observer<Grid>) = config.observeGrid(callback)

    fun observeRecipe(callback: Observer<Recipe>) = config.observeRecipe(callback)

    fun observeContent(callback: Observer<ResourceAmount<ItemVariant>>) = craftingStorage.observeContent(callback)

    fun observeForecast(callback: Observer<ResourceAmount<ItemVariant>>): AutoCloseable {
        markForeCastDirty()
        return forecastHolder.observeValue(callback)
    }

    fun onNeighborUpdate() {
        logger.info("neighbor updated")
        markForeCastDirty()
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
        if (--updateDelay > 0) return
        updateDelay = 60

        if (recipe.isEmpty) {
            forecastHolder.set(EMPTY_ITEM_AMOUNT)
            return
        }
        withOuterTransaction { tx ->
            val res = craftingStorage.resource
            val amount = craftingStorage.extract(recipe.output.resource, recipe.output.amount, tx)
            tx.abort()
            forecastHolder.set(res.toAmount(amount))
        }
    }

    private val apiCaches: Map<Direction, BlockApiCache<Storage<ItemVariant>, Direction>> by lazy {
        (world as? ServerWorld)?.let { world ->
            buildMap {
                for (direction in Direction.values()) {
                    put(direction, BlockApiCache.create(ItemStorage.SIDED, world, pos.offset(direction)))
                }
            }
        } ?: mapOf()
    }

    private fun findStorages(): List<Storage<ItemVariant>> =
        apiCaches.entries.mapNotNull { (direction, cache) -> cache.find(direction) }

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
