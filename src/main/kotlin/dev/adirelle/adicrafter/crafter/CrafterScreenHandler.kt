@file:Suppress("UnstableApiUsage")

package dev.adirelle.adicrafter.crafter

import dev.adirelle.adicrafter.AdiCrafter
import dev.adirelle.adicrafter.crafter.CrafterBlockEntity.Companion.FIRST_INGR_PROP_IDX
import dev.adirelle.adicrafter.crafter.CrafterBlockEntity.Companion.FLUID_PROP_IDX
import dev.adirelle.adicrafter.crafter.CrafterBlockEntity.Companion.FUZZY_PROP_IDX
import dev.adirelle.adicrafter.crafter.CrafterBlockEntity.Companion.GRID_FIRST_SLOT
import dev.adirelle.adicrafter.crafter.CrafterBlockEntity.Companion.GRID_HEIGHT
import dev.adirelle.adicrafter.crafter.CrafterBlockEntity.Companion.GRID_SLOTS
import dev.adirelle.adicrafter.crafter.CrafterBlockEntity.Companion.GRID_WIDTH
import dev.adirelle.adicrafter.crafter.CrafterBlockEntity.Companion.INVENTORY_SIZE
import dev.adirelle.adicrafter.crafter.CrafterBlockEntity.Companion.OUTPUT_SLOT
import dev.adirelle.adicrafter.crafter.CrafterBlockEntity.Companion.POWER_MAX_PROP_IDX
import dev.adirelle.adicrafter.crafter.CrafterBlockEntity.Companion.POWER_PROP_IDX
import dev.adirelle.adicrafter.crafter.CrafterBlockEntity.Companion.PROP_COUNT
import dev.adirelle.adicrafter.crafter.CrafterBlockEntity.Companion.RESULT_SLOT
import dev.adirelle.adicrafter.crafter.recipe.RecipeLoader
import dev.adirelle.adicrafter.utils.extensions.packetSender
import dev.adirelle.adicrafter.utils.extensions.toInt
import dev.adirelle.adicrafter.utils.lazyLogger
import dev.adirelle.adicrafter.utils.withOuterTransaction
import io.github.cottonmc.cotton.gui.SyncedGuiDescription
import io.github.cottonmc.cotton.gui.client.ScreenDrawing
import io.github.cottonmc.cotton.gui.networking.NetworkSide.CLIENT
import io.github.cottonmc.cotton.gui.networking.ScreenNetworking
import io.github.cottonmc.cotton.gui.widget.*
import io.github.cottonmc.cotton.gui.widget.WBar.Direction.RIGHT
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
import net.minecraft.screen.PropertyDelegate
import net.minecraft.screen.ScreenHandlerContext
import net.minecraft.screen.slot.Slot
import net.minecraft.screen.slot.SlotActionType
import net.minecraft.screen.slot.SlotActionType.*
import net.minecraft.text.TranslatableText
import net.minecraft.util.Formatting.GRAY
import net.minecraft.util.Formatting.WHITE
import net.minecraft.util.Identifier
import java.util.function.Consumer

class CrafterScreenHandler(
    syncId: Int,
    playerInventory: PlayerInventory,
    private val context: ScreenHandlerContext
) :
    SyncedGuiDescription(
        CrafterFeature.SCREEN_HANDLER_TYPE,
        syncId,
        playerInventory,
        getBlockInventory(context, INVENTORY_SIZE),
        getBlockPropertyDelegate(context, PROP_COUNT)
    ) {

    private val logger by lazyLogger

    companion object {

        private val FUZZY_MSG_ID = Identifier(AdiCrafter.MOD_ID, "screen-set-fuzzy")
        private val FLUID_MSG_ID = Identifier(AdiCrafter.MOD_ID, "screen-set-fluid")
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

    private val fuzzyToggle = WFuzzyButton()
    private val fluidToggle = WFluidButton()

    private val sendFuzzyMessage =
        CLIENT.packetSender(this, FUZZY_MSG_ID, PacketByteBuf::writeBoolean, PacketByteBuf::readBoolean)
        { setProperty(FUZZY_PROP_IDX, it.toInt()) }

    private val sendFluidMessage =
        CLIENT.packetSender(this, FLUID_MSG_ID, PacketByteBuf::writeBoolean, PacketByteBuf::readBoolean)
        { setProperty(FLUID_PROP_IDX, it.toInt()) }

    private val sendRecipeMessage =
        CLIENT.packetSender(this, RECIPE_MSG_ID, PacketByteBuf::writeIdentifier, PacketByteBuf::readIdentifier)
        { applyRecipe(it) }

    // Client-side constructor
    @Environment(EnvType.CLIENT)
    constructor(syncId: Int, playerInventory: PlayerInventory, initialState: PacketByteBuf)
        : this(syncId, playerInventory, ScreenHandlerContext.EMPTY) {

        val networking = ScreenNetworking.of(this, CLIENT)

        with(fuzzyToggle) {
            onToggle = Consumer(sendFuzzyMessage)
            toggle = initialState.readBoolean()
        }

        with(fluidToggle) {
            onToggle = Consumer(sendFluidMessage)
            toggle = initialState.readBoolean()
        }
    }

    init {
        val root = rootPanel as? WGridPanel ?: throw IllegalStateException()

        val gridSlot = WGridSlotButton(blockInventory, propertyDelegate)
        root.add(gridSlot, 0, 1)

        val resultSlot = WResultSlot(blockInventory, RESULT_SLOT)
        root.add(resultSlot, 5, 2)

        root.add(WSprite(ARROW_TEX), 3, 2, 2, 1)

        val outputSlot = WItemSlot.outputOf(blockInventory, OUTPUT_SLOT)
        outputSlot.isInsertingAllowed = false
        root.add(outputSlot, 7, 2)

        root.add(fuzzyToggle, 7, 0)
        root.add(fluidToggle, 8, 0)

        val powerBar = WBar(POWER_BAR_BACKGROUND, POWER_BAR_FOREGROUND, POWER_PROP_IDX, POWER_MAX_PROP_IDX, RIGHT)
        root.add(powerBar, 4, 4)
        powerBar.setLocation(root.insets.left + 18 * 4, root.insets.top + 18 * 4 - 5)
        powerBar.setSize(18 * 4, 5)

        root.add(createPlayerInventoryPanel(true), 0, 4)

        root.validate(this)
    }

    private inline fun withBlockEntity(crossinline block: (CrafterBlockEntity) -> Unit) {
        context.run { world, pos ->
            (world.getBlockEntity(pos) as? CrafterBlockEntity)?.let {
                block(it)
            }
        }
    }

    override fun close(player: PlayerEntity) {
        super.close(player)
        withBlockEntity {
            it.onScreenHandlerClosed(this)
        }
    }

    fun applyRecipe(recipe: CraftingRecipe) {
        if (!recipe.fits(GRID_WIDTH, GRID_HEIGHT)) return
        sendRecipeMessage(recipe.id)
    }

    private val recipeLoader by lazy { RecipeLoader(this.blockInventory, GRID_FIRST_SLOT, GRID_WIDTH, GRID_HEIGHT) }

    fun applyRecipe(recipeId: Identifier) {
        val recipe = world.recipeManager[recipeId].orElse(null) ?: return
        when (recipe) {
            is ShapedRecipe    -> recipeLoader.load(recipe)
            is ShapelessRecipe -> recipeLoader.load(recipe)
            else               -> logger.debug("unhandled recipe type: {}", recipe)
        }
    }

    private inline fun ifBlockInventory(slotId: Int, block: (Slot) -> Unit) {
        slots.getOrNull(slotId)?.let { slot ->
            if (slot.inventory === blockInventory) {
                block(slot)
            }
        }
    }

    override fun onSlotClick(slotIndex: Int, button: Int, actionType: SlotActionType, player: PlayerEntity) {
        if (world.isClient) return
        ifBlockInventory(slotIndex) { slot ->
            when (slot.index) {
                in GRID_SLOTS -> onGridClick(slot, actionType)
                OUTPUT_SLOT   -> onOutputClick(slot, actionType)
                else          -> logger.debug("ignored slot action: {}", slot.index, actionType)
            }
            return
        }
        super.onSlotClick(slotIndex, button, actionType, player)
    }

    private fun onGridClick(slot: Slot, actionType: SlotActionType) {
        when {
            actionType == PICKUP_ALL           -> blockInventory.clear()
            !slot.stack.isOf(cursorStack.item) -> slot.stack = cursorStack
            else                               -> slot.stack = ItemStack.EMPTY
        }
    }

    private fun onOutputClick(slot: Slot, actionType: SlotActionType) {
        withBlockEntity {
            val storage = it.storage
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

    private class WGridSlotButton(inventory: Inventory, private val props: PropertyDelegate) : WItemSlot(
        inventory,
        GRID_FIRST_SLOT,
        GRID_WIDTH,
        GRID_HEIGHT,
        false
    ) {

        override fun paint(matrices: MatrixStack, x: Int, y: Int, mouseX: Int, mouseY: Int) {
            super.paint(matrices, x, y, mouseX, mouseY)

            for (sx in 0 until GRID_WIDTH) {
                for (sy in 0 until GRID_HEIGHT) {
                    val i = sy * GRID_WIDTH + sx
                    if (props.get(FIRST_INGR_PROP_IDX + i) > 0) {
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

    private class WResultSlot(inv: Inventory, slot: Int) : WItemSlot(inv, slot, 1, 1, false) {

        init {
            isModifiable = false
        }

        override fun canFocus() = false
    }
}
