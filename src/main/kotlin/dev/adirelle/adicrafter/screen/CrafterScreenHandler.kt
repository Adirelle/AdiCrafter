package dev.adirelle.adicrafter.screen

import dev.adirelle.adicrafter.AdiCrafter.CRAFTER_SCREEN_HANDLER
import dev.adirelle.adicrafter.blockentity.CrafterBlockEntity
import dev.adirelle.adicrafter.utils.copyFrom
import io.github.cottonmc.cotton.gui.SyncedGuiDescription
import io.github.cottonmc.cotton.gui.widget.WGridPanel
import io.github.cottonmc.cotton.gui.widget.WItemSlot
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.inventory.CraftingInventory
import net.minecraft.inventory.Inventory
import net.minecraft.inventory.SimpleInventory
import net.minecraft.item.ItemStack
import net.minecraft.recipe.RecipeType
import net.minecraft.screen.slot.SlotActionType
import net.minecraft.util.Identifier
import org.apache.logging.log4j.LogManager
import java.util.*

class CrafterScreenHandler(
    syncId: Int,
    playerInventory: PlayerInventory,
    private val blockEntity: CrafterBlockEntity? = null
) : SyncedGuiDescription(CRAFTER_SCREEN_HANDLER, syncId, playerInventory) {

    companion object {
        private val LOGGER = LogManager.getLogger()
    }

    private val grid = CraftingInventory(this, 3, 3)
    private val result = blockEntity?.result ?: SimpleInventory(1)
    private val buffer = blockEntity?.buffer ?: SimpleInventory(1)
    private val output = blockEntity?.output ?: SimpleInventory(1)

    private var recipeId = Optional.empty<Identifier>()

    init {
        with(rootPanel as WGridPanel) {
            add(WItemSlot.of(grid, 0, 3, 3), 0, 1)

            val resultSlot = WItemSlot.of(result, 0).apply { isModifiable = false }
            add(resultSlot, 4, 2)

            val bufferSlot = WItemSlot.of(buffer, 0).apply { isModifiable = false }
            add(bufferSlot, 8, 0)

            val outputSlot = WItemSlot.outputOf(output, 0).apply { isInsertingAllowed = false }
            add(outputSlot, 6, 2)

            add(createPlayerInventoryPanel(true), 0, 4, 9, 4)
        }
        rootPanel.validate(this)
    }

    // private val outputSlot = slots.first { it.inventory === result }

    init {
        blockEntity?.let {
            recipeId = it.recipeIdentifier
            preventContentUpdates = true
            grid.copyFrom(it.grid)
            preventContentUpdates = false
        }
    }

    private var preventContentUpdates = false

    override fun onContentChanged(inventory: Inventory) {
        if (preventContentUpdates) return
        super.onContentChanged(inventory)
        if (inventory !== grid || blockEntity === null) return
        val recipe = world.recipeManager.getFirstMatch(RecipeType.CRAFTING, grid, world)
        val newId = recipe.map { it.id }
        if (newId == recipeId) return
        LOGGER.info("reciped changed: {}", recipe)
        recipeId = newId
        blockEntity.grid.copyFrom(grid)
        result.setStack(0, recipe.map { it.output }.orElse(ItemStack.EMPTY))
        blockEntity.recipeIdentifier = newId
    }

    override fun onSlotClick(slotIndex: Int, button: Int, actionType: SlotActionType?, player: PlayerEntity?) {
        if (0 <= slotIndex && slotIndex < slots.size) {
            val slot = slots[slotIndex]
            if (slot.inventory === grid) {
                slot.stack = cursorStack.copy().apply { count = 1 }
                return
            }
        }
        super.onSlotClick(slotIndex, button, actionType, player)
    }
}
