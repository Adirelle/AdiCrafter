@file:Suppress("UnstableApiUsage")

package dev.adirelle.adicrafter.crafter

import dev.adirelle.adicrafter.AdiCrafter
import dev.adirelle.adicrafter.crafter.api.CrafterDataAccessor
import dev.adirelle.adicrafter.crafter.api.recipe.RecipeFlags
import dev.adirelle.adicrafter.crafter.impl.CrafterDataAccessorAdapter
import dev.adirelle.adicrafter.crafter.impl.Grid
import dev.adirelle.adicrafter.utils.BitArray
import dev.adirelle.adicrafter.utils.extensions.loadFrom
import dev.adirelle.adicrafter.utils.extensions.packetSender
import dev.adirelle.adicrafter.utils.lazyLogger
import dev.adirelle.adicrafter.utils.mapped
import dev.adirelle.adicrafter.utils.withOuterTransaction
import io.github.cottonmc.cotton.gui.SyncedGuiDescription
import io.github.cottonmc.cotton.gui.client.ScreenDrawing
import io.github.cottonmc.cotton.gui.networking.NetworkSide.CLIENT
import io.github.cottonmc.cotton.gui.widget.*
import io.github.cottonmc.cotton.gui.widget.data.Texture
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant
import net.fabricmc.fabric.api.transfer.v1.item.PlayerInventoryStorage
import net.fabricmc.fabric.api.transfer.v1.storage.Storage
import net.fabricmc.fabric.api.transfer.v1.storage.StorageUtil
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.inventory.Inventory
import net.minecraft.item.ItemStack
import net.minecraft.network.PacketByteBuf
import net.minecraft.recipe.CraftingRecipe
import net.minecraft.recipe.ShapedRecipe
import net.minecraft.recipe.ShapelessRecipe
import net.minecraft.screen.Property
import net.minecraft.screen.slot.Slot
import net.minecraft.screen.slot.SlotActionType
import net.minecraft.screen.slot.SlotActionType.*
import net.minecraft.text.TranslatableText
import net.minecraft.util.Formatting.GRAY
import net.minecraft.util.Formatting.WHITE
import net.minecraft.util.Identifier
import java.util.function.Consumer

class CrafterScreenHandler
private constructor(
    syncId: Int,
    playerInventory: PlayerInventory,
    private val dataAccessor: CrafterDataAccessorAdapter
) :
    SyncedGuiDescription(
        CrafterFeature.SCREEN_HANDLER_TYPE,
        syncId,
        playerInventory,
        dataAccessor.inventory,
        dataAccessor.propertyDelegate
    ) {

    companion object {

        private val FLAGS_MSG_ID = Identifier(AdiCrafter.MOD_ID, "screen-set-flags")
        private val RECIPE_MSG_ID = Identifier(AdiCrafter.MOD_ID, "screen-apply-recipe")

        private val TEXTURES = Texture(Identifier(AdiCrafter.MOD_ID, "textures/gui/crafter.png"))
        private val ARROW_TEX = TEXTURES.withUv(0f, 0f, 1f, 17f / 53)
        private val FLUID_OFF_TEX = TEXTURES.withUv(0f, 18f / 54, 17f / 35, 35f / 53)
        private val FLUID_ON_TEX = TEXTURES.withUv(18f / 35, 18f / 54, 1f, 35f / 53)
        private val FUZZY_OFF_TEX = TEXTURES.withUv(0f, 36f / 54, 17f / 35, 1f)
        private val FUZZY_ON_TEX = TEXTURES.withUv(18f / 35, 36f / 54, 1f, 1f)

        private val BAR_TEXTURES = Texture(Identifier("minecraft", "textures/gui/bars.png"))
        private val POWER_BAR_BACKGROUND = BAR_TEXTURES.withUv(0f, 20f / 255, 182f / 255, 25f / 255)
        private val POWER_BAR_FOREGROUND = BAR_TEXTURES.withUv(0f, 25f / 255, 182f / 255, 30f / 255)
    }

    private val logger by lazyLogger

    private var recipeFlags: RecipeFlags by dataAccessor.recipeFlags.mapped(RecipeFlags::of, RecipeFlags::toInt)

    private val fuzzyToggle = WFuzzyButton()
    private val fluidToggle = WFluidButton()

    private val sendRecipeFlagsMessage =
        CLIENT.packetSender(this, FLAGS_MSG_ID, PacketByteBuf::writeInt, PacketByteBuf::readInt)
        { recipeFlags = RecipeFlags.of(it) }

    private val sendRecipeMessage =
        CLIENT.packetSender(this, RECIPE_MSG_ID, PacketByteBuf::writeIdentifier, PacketByteBuf::readIdentifier)
        { applyRecipe(it) }

    // Server-side constructor
    constructor(syncId: Int, playerInventory: PlayerInventory, dataAccessor: CrafterDataAccessor) :
        this(syncId, playerInventory, CrafterDataAccessorAdapter(dataAccessor))

    // Client-side constructor
    @Environment(EnvType.CLIENT)
    constructor(syncId: Int, playerInventory: PlayerInventory, initialState: PacketByteBuf) :
        this(
            syncId,
            playerInventory,
            CrafterDataAccessorAdapter(
                CrafterDataAccessor.Dummy(
                    initialState.readBoolean(),
                    initialState.readBoolean()
                )
            )
        ) {

        recipeFlags = RecipeFlags.fromPacket(initialState)

        fun wireToggle(button: WToggleButton, flag: RecipeFlags) {
            with(button) {
                onToggle = Consumer { enabled ->
                    sendRecipeFlagsMessage(recipeFlags.set(flag, enabled).toInt())
                }
                toggle = flag in recipeFlags
            }
        }

        wireToggle(fuzzyToggle, RecipeFlags.FUZZY)
        wireToggle(fluidToggle, RecipeFlags.FLUIDS)
    }

    init {
        val root = rootPanel as? WGridPanel ?: throw IllegalStateException()

        root.add(fuzzyToggle, 7, 0)
        root.add(fluidToggle, 8, 0)

        val gridSlot = WGridSlotButton(dataAccessor.grid, Grid.WIDTH, Grid.HEIGHT, dataAccessor.missingIngredients)
        root.add(gridSlot, 0, 1)

        val resultSlot = WResultSlot(dataAccessor.result)
        root.add(resultSlot, 5, 2)

        root.add(WSprite(ARROW_TEX), 3, 2, 2, 1)

        val outputSlot = WItemSlot.outputOf(dataAccessor.forecast, 0)
        outputSlot.isInsertingAllowed = false
        root.add(outputSlot, 7, 2)

        if (dataAccessor.hasPowerBar) {
            val powerBar = WBar(
                POWER_BAR_BACKGROUND,
                POWER_BAR_FOREGROUND,
                CrafterDataAccessorAdapter.POWER_AMOUNT_PROP,
                CrafterDataAccessorAdapter.POWER_CAPACITY_PROP,
                WBar.Direction.RIGHT
            ).withTooltip("gui.adicrafter.powerBar.tooltip.title")
            root.add(powerBar, 5, 4)
            powerBar.setLocation(root.insets.left + 18 * 4, root.insets.top + 18 * 4 - 5)
            powerBar.setSize(18 * 3, 5)
        }

        if (dataAccessor.fuel != null) {
            val generatorSlot = WItemSlot.of(dataAccessor.fuel, 0)
            root.add(generatorSlot, 5, 0)
        }

        root.add(createPlayerInventoryPanel(true), 0, 4)

        root.validate(this)
    }

    override fun close(player: PlayerEntity) {
        super.close(player)
        dataAccessor.onScreenHandlerClosed(this)
    }

    fun applyRecipe(recipe: CraftingRecipe) {
        if (!recipe.fits(Grid.WIDTH, Grid.HEIGHT)) return
        sendRecipeMessage(recipe.id)
    }

    private fun applyRecipe(recipeId: Identifier) {
        val recipe = world.recipeManager[recipeId].orElse(null) ?: return
        when (recipe) {
            is ShapedRecipe    -> dataAccessor.grid.loadFrom(recipe)
            is ShapelessRecipe -> dataAccessor.grid.loadFrom(recipe)
            else               -> logger.warn("unhandled recipe type: {}", recipe)
        }
    }

    override fun onSlotClick(slotIndex: Int, button: Int, actionType: SlotActionType, player: PlayerEntity) {
        if (!handleSlotClick(slotIndex, actionType)) {
            super.onSlotClick(slotIndex, button, actionType, player)
        }
    }

    private fun handleSlotClick(slotIndex: Int, actionType: SlotActionType): Boolean {
        if (world.isClient) return false
        val slot = slots.getOrNull(slotIndex) ?: return false
        when (slot.inventory) {
            dataAccessor.grid     -> onGridClick(slot, actionType)
            dataAccessor.forecast -> onOutputClick(slot, actionType)
            else                  -> return false
        }
        return true
    }

    private fun onGridClick(slot: Slot, actionType: SlotActionType) {
        when {
            actionType == PICKUP_ALL           -> blockInventory.clear()
            !slot.stack.isOf(cursorStack.item) -> slot.stack = cursorStack
            else                               -> slot.stack = ItemStack.EMPTY
        }
    }

    private fun onOutputClick(slot: Slot, actionType: SlotActionType) {
        val storage = dataAccessor.crafter
        withOuterTransaction { tx ->
            val moved = when (actionType) {
                PICKUP     -> pickupOutput(storage, slot.stack.count.toLong(), tx)
                PICKUP_ALL -> pickupOutput(storage, slot.stack.maxCount.toLong(), tx)
                QUICK_MOVE -> quickMoveOutput(storage, slot.stack.maxCount.toLong(), tx)
                else       -> {
                    logger.debug("ignored output action: {}", actionType)
                    0
                }
            }
            if (moved > 0) tx.commit()
        }
    }

    private fun pickupOutput(storage: Storage<ItemVariant>, maxAmount: Long, tx: Transaction) =
        StorageUtil.move(storage, PlayerInventoryStorage.getCursorStorage(this), { true }, maxAmount, tx)

    private fun quickMoveOutput(storage: Storage<ItemVariant>, maxAmount: Long, tx: Transaction) =
        playerInventory?.let { inv ->
            StorageUtil.move(storage, PlayerInventoryStorage.of(inv), { true }, maxAmount, tx)
        } ?: 0L

    private class WFuzzyButton : WToggleButton(FUZZY_ON_TEX, FUZZY_OFF_TEX) {

        override fun addTooltip(tooltip: TooltipBuilder) {
            tooltip.add(
                TranslatableText("gui.adicrafter.fuzzy.tooltip.title")
                    .formatted(WHITE),
                TranslatableText(
                    if (toggle) "gui.adicrafter.fuzzy.tooltip.enabled"
                    else "gui.adicrafter.fuzzy.tooltip.disabled"
                )
                    .formatted(GRAY),
            )
        }
    }

    private class WGridSlotButton(
        inventory: Inventory,
        private val gridWidth: Int,
        private val gridHeight: Int,
        private val missingProperty: Property
    ) : WItemSlot(inventory, 0, gridWidth, gridHeight, false) {

        override fun paint(matrices: MatrixStack, x: Int, y: Int, mouseX: Int, mouseY: Int) {
            super.paint(matrices, x, y, mouseX, mouseY)

            val missingIngredients = BitArray.fromInt(gridWidth * gridHeight, missingProperty.get())
            for (sx in 0 until gridWidth) {
                for (sy in 0 until gridHeight) {
                    val i = sy * gridWidth + sx
                    if (missingIngredients[i]) {
                        ScreenDrawing.coloredRect(matrices, x + sx * 18, y + sy * 18, 18, 18, 0x44FF0000)
                    }
                }
            }
        }
    }

    private class WFluidButton : WToggleButton(FLUID_ON_TEX, FLUID_OFF_TEX) {

        override fun addTooltip(tooltip: TooltipBuilder) {
            tooltip.add(
                TranslatableText("gui.adicrafter.fluid.tooltip.title")
                    .formatted(WHITE),
                TranslatableText(
                    if (toggle) "gui.adicrafter.fluid.tooltip.enabled"
                    else "gui.adicrafter.fluid.tooltip.disabled"
                )
                    .formatted(GRAY),
            )
        }
    }

    private class WResultSlot(inv: Inventory) : WItemSlot(inv, 0, 1, 1, false) {

        init {
            isModifiable = false
        }

        override fun canFocus() = false
    }
}
