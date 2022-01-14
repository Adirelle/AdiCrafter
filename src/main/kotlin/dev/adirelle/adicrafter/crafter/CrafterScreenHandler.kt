@file:Suppress("UnstableApiUsage")

package dev.adirelle.adicrafter.crafter

import dev.adirelle.adicrafter.AdiCrafter
import dev.adirelle.adicrafter.crafter.CrafterBlockEntity.Companion.FLUID_PROP_IDX
import dev.adirelle.adicrafter.crafter.CrafterBlockEntity.Companion.FUZZY_PROP_IDX
import dev.adirelle.adicrafter.crafter.CrafterBlockEntity.Companion.GRID_FIRST_SLOT
import dev.adirelle.adicrafter.crafter.CrafterBlockEntity.Companion.GRID_HEIGHT
import dev.adirelle.adicrafter.crafter.CrafterBlockEntity.Companion.GRID_SLOTS
import dev.adirelle.adicrafter.crafter.CrafterBlockEntity.Companion.GRID_WIDTH
import dev.adirelle.adicrafter.crafter.CrafterBlockEntity.Companion.INVENTORY_SIZE
import dev.adirelle.adicrafter.crafter.CrafterBlockEntity.Companion.OUTPUT_SLOT
import dev.adirelle.adicrafter.crafter.CrafterBlockEntity.Companion.PROP_COUNT
import dev.adirelle.adicrafter.crafter.CrafterBlockEntity.Companion.RESULT_SLOT
import dev.adirelle.adicrafter.utils.extensions.toInt
import dev.adirelle.adicrafter.utils.lazyLogger
import dev.adirelle.adicrafter.utils.withOuterTransaction
import io.github.cottonmc.cotton.gui.SyncedGuiDescription
import io.github.cottonmc.cotton.gui.networking.NetworkSide.CLIENT
import io.github.cottonmc.cotton.gui.networking.NetworkSide.SERVER
import io.github.cottonmc.cotton.gui.networking.ScreenNetworking
import io.github.cottonmc.cotton.gui.widget.*
import io.github.cottonmc.cotton.gui.widget.data.Texture
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant
import net.fabricmc.fabric.api.transfer.v1.item.PlayerInventoryStorage
import net.fabricmc.fabric.api.transfer.v1.storage.Storage
import net.fabricmc.fabric.api.transfer.v1.storage.StorageUtil
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.inventory.Inventory
import net.minecraft.item.ItemStack
import net.minecraft.network.PacketByteBuf
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

        private val TEXTURES = Texture(Identifier(AdiCrafter.MOD_ID, "textures/gui/crafter.png"))
        private val ARROW_TEX = TEXTURES.withUv(0f, 0f, 1f, 17f / 53)
        private val FLUID_OFF_TEX = TEXTURES.withUv(0f, 18f / 54, 17f / 35, 35f / 53)
        private val FLUID_ON_TEX = TEXTURES.withUv(18f / 35, 18f / 54, 1f, 35f / 53)
        private val FUZZY_OFF_TEX = TEXTURES.withUv(0f, 36f / 54, 17f / 35, 1f)
        private val FUZZY_ON_TEX = TEXTURES.withUv(18f / 35, 36f / 54, 1f, 1f)
    }

    private val fuzzyToggle = WFuzzyButton()
    private val fluidToggle = WFluidButton()

    // Client-side constructor
    @Environment(EnvType.CLIENT)
    constructor(syncId: Int, playerInventory: PlayerInventory, initialState: PacketByteBuf)
        : this(syncId, playerInventory, ScreenHandlerContext.EMPTY) {

        val networking = ScreenNetworking.of(this, CLIENT)

        with(fuzzyToggle) {
            onToggle = Consumer { enabled ->
                networking.send(FUZZY_MSG_ID) { it.writeBoolean(enabled) }
            }
            toggle = initialState.readBoolean()
        }

        with(fluidToggle) {
            onToggle = Consumer { enabled ->
                networking.send(FLUID_MSG_ID) { it.writeBoolean(enabled) }
            }
            toggle = initialState.readBoolean()
        }
    }

    init {
        val root = rootPanel as? WGridPanel ?: throw IllegalStateException()

        val gridSlot = WItemSlot.of(blockInventory, GRID_FIRST_SLOT, GRID_WIDTH, GRID_HEIGHT)
        root.add(gridSlot, 0, 1)

        val resultSlot = WResultSlot(blockInventory, RESULT_SLOT)
        root.add(resultSlot, 5, 2)

        root.add(WSprite(ARROW_TEX), 3, 2, 2, 1)

        val outputSlot = WItemSlot.outputOf(blockInventory, OUTPUT_SLOT)
        outputSlot.isInsertingAllowed = false
        root.add(outputSlot, 7, 2)

        root.add(fuzzyToggle, 7, 0)
        root.add(fluidToggle, 8, 0)

        root.add(createPlayerInventoryPanel(true), 0, 4)

        root.validate(this)

        // Server-side init
        context.run { _, _ ->
            with(ScreenNetworking.of(this, SERVER)) {
                receive(FUZZY_MSG_ID) { buf ->
                    setProperty(FUZZY_PROP_IDX, buf.readBoolean().toInt())
                }
                receive(FLUID_MSG_ID) { buf ->
                    setProperty(FLUID_PROP_IDX, buf.readBoolean().toInt())
                }
            }
        }
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
