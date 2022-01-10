@file:Suppress("UnstableApiUsage")

package dev.adirelle.adicrafter.crafter

import dev.adirelle.adicrafter.crafter.internal.Grid
import dev.adirelle.adicrafter.utils.expectExactSize
import dev.adirelle.adicrafter.utils.extension.*
import dev.adirelle.adicrafter.utils.general.extensions.toStack
import io.github.cottonmc.cotton.gui.SyncedGuiDescription
import io.github.cottonmc.cotton.gui.widget.WGridPanel
import io.github.cottonmc.cotton.gui.widget.WItemSlot
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant
import net.fabricmc.fabric.api.transfer.v1.item.PlayerInventoryStorage
import net.fabricmc.fabric.api.transfer.v1.storage.Storage
import net.fabricmc.fabric.api.transfer.v1.storage.StorageUtil
import net.fabricmc.fabric.api.transfer.v1.storage.base.ResourceAmount
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.inventory.Inventory
import net.minecraft.inventory.SimpleInventory
import net.minecraft.item.ItemStack
import net.minecraft.screen.slot.Slot
import net.minecraft.screen.slot.SlotActionType
import net.minecraft.screen.slot.SlotActionType.*
import kotlin.math.min

class CrafterScreenHandler(
    syncId: Int, playerInventory: PlayerInventory,
    blockEntity: CrafterBlockEntity? = null
) : SyncedGuiDescription(Crafter.SCREEN_HANDLER_TYPE, syncId, playerInventory) {

    private val grid = SimpleInventory(CrafterBlockEntity.GRID_SIZE)
    private val result = SimpleInventory(1)
    private val buffer = SimpleInventory(1)
    private val forecast = SimpleInventory(1)

    private val crafter: Storage<ItemVariant>? = blockEntity?.storage

    init {
        with(rootPanel as WGridPanel) {
            val gridSlot = WItemSlot.of(grid, 0, CrafterBlockEntity.GRID_WIDTH, CrafterBlockEntity.GRID_HEIGHT)
                .apply { isModifiable = false }
            add(gridSlot, 0, 1)

            val resultSlot = WItemSlot.of(result, 0).apply { isModifiable = false }
            add(resultSlot, 4, 2)

            val bufferSlot = WItemSlot.of(buffer, 0).apply { isModifiable = false }
            add(bufferSlot, 8, 0)

            val outputSlot = WItemSlot.outputOf(forecast, 0).apply { isModifiable = false }
            add(outputSlot, 6, 2)

            add(createPlayerInventoryPanel(true), 0, 4)
        }
        rootPanel.validate(this)
    }

    private val subscriptions = blockEntity?.let { onServer(it) } ?: listOf()

    private fun onServer(blockEntity: CrafterBlockEntity): List<AutoCloseable> =
        listOf(
            blockEntity.observeGrid(this::onGridChanged),
            blockEntity.observeRecipe(this::onRecipeChanged),
            blockEntity.observeContent(this::onContentChanged),
            blockEntity.observeForecast(this::onForecastChanged)
        ).also {
            onGridChanged(blockEntity.grid)
            onRecipeChanged(blockEntity.recipe)
            onContentChanged(blockEntity.content)
            onForecastChanged(blockEntity.forecast)
            grid.addListener { onGridEditedLocally(blockEntity) }
        }

    override fun close(player: PlayerEntity) {
        super.close(player)
        subscriptions.close()
    }

    private var updatingGrid = false

    private fun onGridEditedLocally(blockEntity: CrafterBlockEntity) {
        if (updatingGrid) return
        blockEntity.grid = Grid.copyOf(grid.asList())
    }

    private fun onGridChanged(newGrid: Grid) {
        try {
            updatingGrid = true
            expectExactSize(newGrid, grid.size()).forEachIndexed { index, amount ->
                setStack(grid, index, amount)
            }
        } finally {
            updatingGrid = false
        }
    }

    private fun setStack(inventory: Inventory, invSlot: Int, items: ResourceAmount<ItemVariant>) {
        getSlotIndex(inventory, invSlot).ifPresent { slotIndex ->
            slots[slotIndex].stack = items.toStack()
        }
    }

    private fun onRecipeChanged(recipe: Recipe) {
        setStack(result, 0, recipe.output)
    }

    private fun onContentChanged(content: ResourceAmount<ItemVariant>) {
        setStack(buffer, 0, content)
    }

    private fun onForecastChanged(newForecast: ResourceAmount<ItemVariant>) {
        setStack(forecast, 0, newForecast)
    }

    override fun onSlotClick(slotIndex: Int, button: Int, actionType: SlotActionType, player: PlayerEntity) {
        if (world.isClient) return
        slots.getOrNull(slotIndex)?.let { slot ->
            return when (slot.inventory) {
                grid     -> onGridClick(slot, actionType)
                forecast -> crafter?.let { onOutputClick(it, actionType) } ?: Unit
                else     -> super.onSlotClick(slotIndex, button, actionType, player)
            }
        }
        super.onSlotClick(slotIndex, button, actionType, player)
    }

    private fun onGridClick(slot: Slot, actionType: SlotActionType) {
        if (actionType == PICKUP) {
            if (!cursorStack.isEmpty && (slot.stack.isEmpty || !ItemStack.canCombine(cursorStack, slot.stack))) {
                slot.stack = cursorStack.copy().apply { count = 1 }
            } else {
                slot.stack = ItemStack.EMPTY
            }
            return
        }

        if (actionType == QUICK_CRAFT) {
            slot.stack = cursorStack.copy().apply { count = 1 }
            return
        }

        if (actionType == QUICK_MOVE) {
            slot.stack = ItemStack.EMPTY
            return
        }
    }

    private fun onOutputClick(crafter: Storage<ItemVariant>, actionType: SlotActionType) =
        when (actionType) {
            PICKUP     ->
                withOuterTransaction { tx ->
                    val cursor = PlayerInventoryStorage.getCursorStorage(this)
                    StorageUtil.move(
                        crafter,
                        cursor,
                        { canExtract(cursor.resource, it) },
                        min(cursor.capacity - cursor.amount, forecast[0].count.toLong()),
                        tx

                    )
                    tx.commit()
                }
            PICKUP_ALL ->
                withOuterTransaction { tx ->
                    val cursor = PlayerInventoryStorage.getCursorStorage(this)
                    StorageUtil.move(
                        crafter,
                        cursor,
                        { canExtract(cursor.resource, it) },
                        cursor.capacity - cursor.amount,
                        tx
                    )
                    tx.commit()
                }
            QUICK_MOVE ->
                withOuterTransaction { tx ->
                    StorageUtil.move(
                        crafter,
                        PlayerInventoryStorage.of(playerInventory),
                        { true },
                        Long.MAX_VALUE,
                        tx
                    )
                    tx.commit()
                }
            else       ->
                Unit
        }

}
