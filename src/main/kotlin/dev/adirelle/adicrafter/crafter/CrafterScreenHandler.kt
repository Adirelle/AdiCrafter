@file:Suppress("UnstableApiUsage")

package dev.adirelle.adicrafter.crafter

import dev.adirelle.adicrafter.AdiCrafter
import dev.adirelle.adicrafter.crafter.CrafterBlockEntity.Companion.FUZZY_PROP_IDX
import dev.adirelle.adicrafter.crafter.CrafterBlockEntity.Companion.GRID_FIRST_SLOT
import dev.adirelle.adicrafter.crafter.CrafterBlockEntity.Companion.GRID_HEIGHT
import dev.adirelle.adicrafter.crafter.CrafterBlockEntity.Companion.GRID_SLOTS
import dev.adirelle.adicrafter.crafter.CrafterBlockEntity.Companion.GRID_WIDTH
import dev.adirelle.adicrafter.crafter.CrafterBlockEntity.Companion.INVENTORY_SIZE
import dev.adirelle.adicrafter.crafter.CrafterBlockEntity.Companion.OUTPUT_SLOT
import dev.adirelle.adicrafter.crafter.CrafterBlockEntity.Companion.PROP_COUNT
import dev.adirelle.adicrafter.crafter.CrafterBlockEntity.Companion.RESULT_SLOT
import dev.adirelle.adicrafter.utils.extensions.toBoolean
import dev.adirelle.adicrafter.utils.extensions.toInt
import dev.adirelle.adicrafter.utils.lazyLogger
import dev.adirelle.adicrafter.utils.withOuterTransaction
import io.github.cottonmc.cotton.gui.SyncedGuiDescription
import io.github.cottonmc.cotton.gui.networking.NetworkSide.CLIENT
import io.github.cottonmc.cotton.gui.networking.NetworkSide.SERVER
import io.github.cottonmc.cotton.gui.networking.ScreenNetworking
import io.github.cottonmc.cotton.gui.widget.WGridPanel
import io.github.cottonmc.cotton.gui.widget.WItemSlot
import io.github.cottonmc.cotton.gui.widget.WToggleButton
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant
import net.fabricmc.fabric.api.transfer.v1.item.PlayerInventoryStorage
import net.fabricmc.fabric.api.transfer.v1.storage.Storage
import net.fabricmc.fabric.api.transfer.v1.storage.StorageUtil
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.item.ItemStack
import net.minecraft.screen.ScreenHandler
import net.minecraft.screen.ScreenHandlerContext
import net.minecraft.screen.ScreenHandlerListener
import net.minecraft.screen.slot.Slot
import net.minecraft.screen.slot.SlotActionType
import net.minecraft.screen.slot.SlotActionType.*
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
    ),
    ScreenHandlerListener {

    private val logger by lazyLogger

    companion object {

        private val FUZZY_MSG_ID = Identifier(AdiCrafter.MOD_ID, "screen-set-fuzzy")
    }

    private val clientNetworking = ScreenNetworking.of(this, CLIENT)
    private val serverNetworking = ScreenNetworking.of(this, SERVER)

    private val fuzzyButton = WToggleButton().apply {
        onToggle = Consumer { enabled ->
            clientNetworking.send(FUZZY_MSG_ID) { it.writeBoolean(enabled) }
        }
    }

    init {
        val root = rootPanel as? WGridPanel ?: throw IllegalStateException()

        val gridSlot = WItemSlot.of(blockInventory, GRID_FIRST_SLOT, GRID_WIDTH, GRID_HEIGHT)
        root.add(gridSlot, 0, 1)

        val resultSlot = WItemSlot.of(blockInventory, RESULT_SLOT)
        resultSlot.isModifiable = false
        root.add(resultSlot, 4, 2)

        val outputSlot = WItemSlot.outputOf(blockInventory, OUTPUT_SLOT)
        outputSlot.isInsertingAllowed = false
        root.add(outputSlot, 6, 2)

        root.add(fuzzyButton, 8, 0)
        fuzzyButton.toggle = propertyDelegate.get(FUZZY_PROP_IDX).toBoolean()

        root.add(createPlayerInventoryPanel(true), 0, 4)

        root.validate(this)

        serverNetworking.receive(FUZZY_MSG_ID) { buf ->
            setProperty(FUZZY_PROP_IDX, buf.readBoolean().toInt())
            sendContentUpdates()
        }

        addListener(this)
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

    override fun onSlotUpdate(handler: ScreenHandler, slotId: Int, stack: ItemStack) {}

    override fun onPropertyUpdate(handler: ScreenHandler, property: Int, value: Int) {
        when (property) {
            FUZZY_PROP_IDX -> {
                logger.debug("fuzzy property updated to {}", value.toBoolean())
                fuzzyButton.toggle = value.toBoolean()
            }
            else           ->
                logger.debug("ignored property update: {} -> {}", property, value)
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

}
