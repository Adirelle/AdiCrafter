@file:Suppress("UnstableApiUsage")

package dev.adirelle.adicrafter.crafter

import dev.adirelle.adicrafter.AdiCrafter
import dev.adirelle.adicrafter.crafter.api.Crafter
import dev.adirelle.adicrafter.crafter.api.CrafterDataAccessor
import dev.adirelle.adicrafter.crafter.api.power.PowerSource
import dev.adirelle.adicrafter.crafter.api.recipe.Recipe
import dev.adirelle.adicrafter.crafter.api.recipe.RecipeFlags
import dev.adirelle.adicrafter.crafter.api.storage.StorageProvider
import dev.adirelle.adicrafter.crafter.impl.BufferedCrafter
import dev.adirelle.adicrafter.crafter.impl.Grid
import dev.adirelle.adicrafter.crafter.impl.SimulatingCrafter
import dev.adirelle.adicrafter.utils.*
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant
import net.fabricmc.fabric.api.transfer.v1.storage.Storage
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext
import net.fabricmc.fabric.api.util.NbtType
import net.minecraft.block.BlockState
import net.minecraft.block.entity.BlockEntity
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.inventory.Inventory
import net.minecraft.item.BlockItem
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NbtCompound
import net.minecraft.network.PacketByteBuf
import net.minecraft.screen.Property
import net.minecraft.screen.ScreenHandler
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.server.world.ServerWorld
import net.minecraft.text.Text
import net.minecraft.text.TranslatableText
import net.minecraft.util.ItemScatterer
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3d
import net.minecraft.util.registry.Registry
import net.minecraft.world.World

open class CrafterBlockEntity(
    blockEntityType: BlockEntityType<CrafterBlockEntity>,
    pos: BlockPos,
    state: BlockState,
    val powerSource: PowerSource,
    private val recipeFactoryProvider: (RecipeFlags) -> Recipe.Factory,
    private val storageProviderProvider: (World?, BlockPos) -> StorageProvider
) :
    BlockEntity(blockEntityType, pos, state),
    ExtendedScreenHandlerFactory,
    PowerSource.Listener,
    Crafter.Listener,
    Droppable,
    Tickable {

    private val logger by lazyLogger

    companion object {

        private val forecastUpdatePeriod by lazy { AdiCrafter.config.crafter.updatePeriod }

        private const val GRID_NBT_KEY = "Grid"
        private const val CONTENT_NBT_KEY = "Content"
        private const val FLAGS_NBT_KEY = "Flags"
        private const val GENERATOR_NBT_KEY = "Generator"
    }

    val dataAccessor: CrafterDataAccessor = DataAccessor()

    val id by lazy { BlockEntityType.getId(this.type)!! }

    private val grid = Grid.create(::markCrafterDirty)

    private var recipeFlags = RecipeFlags.NONE
        set(value) {
            field = value
            markCrafterDirty()
        }

    private var dirtyCrafter = true
    private var recipe: Recipe = Recipe.EMPTY
    private var crafter: Crafter = Crafter.EMPTY

    private var forecastTimeout = 0L
    private var forecast = 0L
    private val missingIngredients = BitArray.of(Grid.SIZE)

    private val openScreenHandlers = ArrayList<ScreenHandler>(2)

    private val storageProvider by lazy { storageProviderProvider(world, pos) }

    private val titleKey by lazy { "block.adicrafter.${id.path}" }

    override fun getDisplayName(): Text = TranslatableText(titleKey)

    override fun createMenu(syncId: Int, playerInventory: PlayerInventory, player: PlayerEntity): ScreenHandler {
        val handler = CrafterScreenHandler(syncId, playerInventory, this.dataAccessor)
        openScreenHandlers.add(handler)
        return handler
    }

    override fun writeScreenOpeningData(player: ServerPlayerEntity, buf: PacketByteBuf) {
        buf.writeBoolean(powerSource.hasPowerBar())
        buf.writeBoolean(powerSource.asInventory() != null)
        recipeFlags.writeToPacket(buf)
    }

    fun onScreenHandlerClosed(handler: ScreenHandler) {
        openScreenHandlers.remove(handler)
    }

    override fun readNbt(nbt: NbtCompound) {
        super.readNbt(nbt)

        grid.readFromNbt(nbt.getList(GRID_NBT_KEY, NbtType.COMPOUND))
        crafter.readFromNbt(nbt.getCompound(CONTENT_NBT_KEY))
        recipeFlags = RecipeFlags.fromNbt(nbt.getInt(FLAGS_NBT_KEY))
        powerSource.readFromNbt(nbt.getCompound(GENERATOR_NBT_KEY))
    }

    override fun writeNbt(nbt: NbtCompound) {
        super.writeNbt(nbt)

        nbt.put(GRID_NBT_KEY, grid.toNbt())
        nbt.put(CONTENT_NBT_KEY, crafter.toNbt())
        nbt.put(FLAGS_NBT_KEY, recipeFlags.toNbt())
        nbt.put(GENERATOR_NBT_KEY, powerSource.toNbt())
    }

    private fun markCrafterDirty() {
        dirtyCrafter = true
    }

    private fun markForecastDirty() {
        forecastTimeout = 0
    }

    override fun onCrafterUpdate() {
        markForecastDirty()
    }

    override fun onPowerChanged() {
        markForecastDirty()
    }

    override fun tick(world: World): Boolean {
        val crafterUpdated = updateCrafter()
        val powerUpdated = powerSource.tick(world)
        val forecastUpdated = updateForecast()
        if (crafterUpdated || powerUpdated || forecastUpdated) {
            updateScreenHandlers()
            markDirty()
            return true
        }
        return false
    }

    override fun getDroppedStacks(): List<ItemStack> =
        buildList {
            addAll(crafter.getDroppedStacks())
            addAll(powerSource.getDroppedStacks())

            val item = ItemStack(Registry.ITEM[id])
            BlockItem.setBlockEntityNbt(item, type, createNbt())
            add(item)
        }

    private fun updateScreenHandlers() {
        openScreenHandlers.forEach { it.sendContentUpdates() }
    }

    private fun updateCrafter(): Boolean {
        if (!dirtyCrafter) return false
        val world = world as? ServerWorld ?: return false

        scatterItems(crafter.getDroppedStacks())

        dirtyCrafter = false

        recipe = recipeFactoryProvider(recipeFlags).create(world, grid.asList())

        val internalCrafter = recipe.createCrafter(storageProvider, this)
        val bufferedCrafter = BufferedCrafter(internalCrafter, this)
        crafter = SimulatingCrafter(bufferedCrafter)

        markForecastDirty()

        return true
    }

    private fun scatterItems(stacks: Iterable<ItemStack>) {
        val pos = Vec3d.ofCenter(pos.up())
        stacks.forEach {
            ItemScatterer.spawn(world, pos.x, pos.y, pos.z, it)
        }
    }

    private fun updateForecast(): Boolean {
        if (--forecastTimeout > 0) return false

        forecast = with(recipe.output) { crafter.simulateExtract(resource, amount, null) }
        forecastTimeout = forecastUpdatePeriod
        updateMissingIngredients()

        return true
    }

    private fun updateMissingIngredients() {
        if (recipe.isEmpty() || crafter.amount > 0) {
            missingIngredients.clear()
            return
        }
        withOuterTransaction { tx ->
            for ((i, stack) in grid.withIndex()) {
                missingIngredients[i] = crafter
                    .findIngredientFor(stack.item)
                    .map { it.extractFrom(storageProvider, 1, tx) == 0L }
                    .orElse(false)
            }
            tx.abort()
        }
    }

    private inner class DataAccessor : CrafterDataAccessor {

        override val grid: Inventory
            by this@CrafterBlockEntity::grid

        override val crafter: Storage<ItemVariant>
            by this@CrafterBlockEntity::crafter

        override val forecast: Inventory =
            object : ReadonlyInventory {
                private val forecast by this@CrafterBlockEntity::forecast
                override fun size() = 1
                override fun isEmpty() = forecast == 0L
                override fun getStack(slot: Int) = recipe.output.resource.toStack(forecast.toInt())
                override fun onOpen(player: PlayerEntity) {
                    markForecastDirty()
                }
            }

        override val result: Inventory =
            object : ReadonlyInventory {
                override fun size() = 1
                override fun isEmpty() = recipe.output.resource.isBlank
                override fun getStack(slot: Int) = with(recipe.output) {
                    resource.toStack(amount.toInt())
                }
            }

        override val fuel: Inventory? =
            powerSource.asInventory()

        override val hasPowerBar: Boolean =
            powerSource.hasPowerBar()

        override val recipeFlags: Property =
            property(
                { this@CrafterBlockEntity.recipeFlags.toInt() },
                { this@CrafterBlockEntity.recipeFlags = RecipeFlags.of(it) }
            )

        override val missingIngredients: Property =
            property { this@CrafterBlockEntity.missingIngredients.toInt() }

        override val powerAmount: Property =
            property { powerSource.amount.toInt() }

        override val powerCapacity: Property =
            property { powerSource.capacity.toInt() }

        override fun onScreenHandlerClosed(handler: ScreenHandler) {
            this@CrafterBlockEntity.onScreenHandlerClosed(handler)
        }

        override fun craft(amount: Int, tx: TransactionContext?): ItemStack =
            withNestedTransaction(tx) { nested ->
                val extracted = crafter.extract(recipe.output.resource, amount.toLong(), nested)
                nested.commit()
                recipe.output.resource.toStack(extracted.toInt())
            }
    }
}
