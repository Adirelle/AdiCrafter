@file:Suppress("UnstableApiUsage")

package dev.adirelle.adicrafter.crafter

import dev.adirelle.adicrafter.crafter.api.Crafter
import dev.adirelle.adicrafter.crafter.api.CrafterDataAccessor
import dev.adirelle.adicrafter.crafter.api.power.PowerGenerator
import dev.adirelle.adicrafter.crafter.api.recipe.Recipe
import dev.adirelle.adicrafter.crafter.api.recipe.RecipeFlags
import dev.adirelle.adicrafter.crafter.api.storage.StorageProvider
import dev.adirelle.adicrafter.crafter.impl.BufferedCrafter
import dev.adirelle.adicrafter.crafter.impl.Grid
import dev.adirelle.adicrafter.utils.*
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant
import net.fabricmc.fabric.api.transfer.v1.storage.Storage
import net.fabricmc.fabric.api.util.NbtType
import net.minecraft.block.BlockState
import net.minecraft.block.entity.BlockEntity
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.inventory.Inventory
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
import net.minecraft.util.registry.Registry
import net.minecraft.world.World

open class CrafterBlockEntity(
    blockEntityType: BlockEntityType<CrafterBlockEntity>,
    pos: BlockPos,
    state: BlockState,
    private val powerGenerator: PowerGenerator,
    private val recipeFactoryProvider: (RecipeFlags) -> Recipe.Factory,
    private val storageProviderProvider: (World?, BlockPos) -> StorageProvider
) :
    BlockEntity(blockEntityType, pos, state),
    ExtendedScreenHandlerFactory,
    Tickable {

    private val logger by lazyLogger

    companion object {

        private const val GRID_NBT_KEY = "Grid"
        private const val CONTENT_NBT_KEY = "Content"
        private const val FLAGS_NBT_KEY = "Flags"
        private const val GENERATOR_NBT_KEY = "Generator"
    }

    val dataAccessor: CrafterDataAccessor = DataAccessor()

    private val grid = Grid.create(::markCrafterDirty)

    private var recipeFlags = RecipeFlags.NONE
        set(value) {
            field = value
            markCrafterDirty()
        }

    private var dirtyCrafter = false
    private fun markCrafterDirty() {
        dirtyCrafter = true
    }

    private var crafter: Crafter = Crafter.EMPTY
        set(value) {
            field = value
            dirtyCrafter = false
            world?.let {
                bufferedCrafter.dropBufferIfOutputMismatchs(it, pos.up())
            }
            markForecastDirty()
        }

    private var bufferedCrafter = BufferedCrafter(::crafter, ::markForecastDirty)

    init {
        powerGenerator.addListener(::markForecastDirty)
    }

    private var dirtyForecast = false
    private fun markForecastDirty() {
        dirtyForecast = true
    }
    private var forecast: ItemStack = ItemStack.EMPTY
        set(value) {
            field = value
            dirtyForecast = false
        }

    private val missingIngredients = BitArray.of(Grid.SIZE)

    private val openScreenHandlers = ArrayList<ScreenHandler>(2)

    private val storageProvider by lazy { storageProviderProvider(world, pos) }

    private val titleKey by lazy {
        val id = Registry.BLOCK_ENTITY_TYPE.getId(blockEntityType)
        "block.adicrafter.${id?.path ?: "crafter"}"
    }

    override fun getDisplayName(): Text = TranslatableText(titleKey)

    override fun createMenu(syncId: Int, playerInventory: PlayerInventory, player: PlayerEntity): ScreenHandler {
        val handler = CrafterScreenHandler(syncId, playerInventory, this.dataAccessor)
        openScreenHandlers.add(handler)
        return handler
    }

    override fun writeScreenOpeningData(player: ServerPlayerEntity, buf: PacketByteBuf) {
        buf.writeBoolean(powerGenerator.hasPowerBar())
        buf.writeBoolean(powerGenerator.asInventory() != null)
        recipeFlags.writeToPacket(buf)
    }

    fun onScreenHandlerClosed(handler: ScreenHandler) {
        openScreenHandlers.remove(handler)
    }

    override fun readNbt(nbt: NbtCompound) {
        super.readNbt(nbt)

        grid.readFromNbt(nbt.getList(GRID_NBT_KEY, NbtType.COMPOUND))
        bufferedCrafter.readFromNbt(nbt.getCompound(CONTENT_NBT_KEY))
        recipeFlags = RecipeFlags.fromNbt(nbt.getInt(FLAGS_NBT_KEY))
        powerGenerator.readFromNbt(nbt.getCompound(GENERATOR_NBT_KEY))
    }

    override fun writeNbt(nbt: NbtCompound) {
        super.writeNbt(nbt)

        nbt.put(GRID_NBT_KEY, grid.toNbt())
        nbt.put(CONTENT_NBT_KEY, bufferedCrafter.toNbt())
        nbt.put(FLAGS_NBT_KEY, recipeFlags.toNbt())
        nbt.put(GENERATOR_NBT_KEY, powerGenerator.toNbt())
    }

    override fun tick(world: World): Boolean {
        val crafterUpdated = updateCrafter()
        val powerUpdated = powerGenerator.tick(world)
        val forecastUpdated = updateForecast()
        if (crafterUpdated || powerUpdated || forecastUpdated) {
            updateScreenHandlers()
            markDirty()
            return true
        }
        return false
    }

    fun dropContent() {
        val world = world as? ServerWorld ?: return
        bufferedCrafter.dropBuffer(world, pos.up())
        ItemScatterer.spawn(world, pos.up(), powerGenerator.asInventory())
    }

    private fun updateScreenHandlers() {
        openScreenHandlers.forEach { it.sendContentUpdates() }
    }

    private fun updateCrafter(): Boolean {
        if (!dirtyCrafter) return false
        val world = world as? ServerWorld ?: return false

        crafter = recipeFactoryProvider(recipeFlags)
            .create(world, grid.asList())
            .createCrafter(storageProvider)

        return true
    }

    private fun updateForecast(): Boolean {
        if (!dirtyForecast || openScreenHandlers.isEmpty()) return false

        forecast = computeForecast()
        updateMissingIngredients()

        return true
    }

    private fun computeForecast(): ItemStack =
        with(bufferedCrafter) {
            resource.toStack(
                simulateExtract(resource, crafter.amount, null).toInt()
            )
        }

    private fun updateMissingIngredients() {
        if (crafter.isResourceBlank || !forecast.isEmpty) {
            missingIngredients.clear()
            return
        }
        withOuterTransaction { tx ->
            for ((i, stack) in grid.withIndex()) {
                missingIngredients[i] = crafter
                    .findIngredientFor(stack.item)
                    .map { it.extractFrom(storageProvider, 1, tx) > 0 }
                    .orElse(false)
            }
            tx.abort()
        }
    }

    private inner class DataAccessor : CrafterDataAccessor {

        override val grid: Inventory
            by this@CrafterBlockEntity::grid

        override val crafter: Storage<ItemVariant>
            by this@CrafterBlockEntity::bufferedCrafter

        override val forecast: Inventory =
            object : ReadonlyInventory {
                private val stack by this@CrafterBlockEntity::forecast
                override fun size() = 1
                override fun isEmpty() = stack.isEmpty
                override fun getStack(slot: Int) = stack.copy()
                override fun onOpen(player: PlayerEntity) {
                    dirtyForecast = true
                }
            }

        override val result: Inventory =
            object : ReadonlyInventory {
                private val crafter by this@CrafterBlockEntity::crafter
                override fun size() = 1
                override fun isEmpty() = crafter.isResourceBlank
                override fun getStack(slot: Int) = with(crafter) {
                    resource.toStack(amount.toInt())
                }
            }

        override val fuel: Inventory? =
            powerGenerator.asInventory()

        override val hasPowerBar: Boolean =
            powerGenerator.hasPowerBar()

        override val recipeFlags: Property =
            property(
                { this@CrafterBlockEntity.recipeFlags.toInt() },
                { this@CrafterBlockEntity.recipeFlags = RecipeFlags.of(it) }
            )

        override val missingIngredients: Property =
            property { this@CrafterBlockEntity.missingIngredients.toInt() }

        override val powerAmount: Property =
            property { powerGenerator.amount.toInt() }

        override val powerCapacity: Property =
            property { powerGenerator.capacity.toInt() }

        override fun onScreenHandlerClosed(handler: ScreenHandler) {
            this@CrafterBlockEntity.onScreenHandlerClosed(handler)
        }
    }
}
