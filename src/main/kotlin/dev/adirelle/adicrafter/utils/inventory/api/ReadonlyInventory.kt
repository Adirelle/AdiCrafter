package dev.adirelle.adicrafter.utils.inventory.api

import dev.adirelle.adicrafter.utils.callback.api.Callback
import dev.adirelle.adicrafter.utils.lazyLogger
import dev.adirelle.adicrafter.utils.storage.slotarray.api.Version
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.inventory.SidedInventory
import net.minecraft.item.ItemStack
import net.minecraft.item.ItemStack.EMPTY
import net.minecraft.util.math.Direction

interface ReadonlyInventory : SidedInventory {

    fun getVersion(): Version
    val onContentChanged: Callback<Long>

    override fun clear() {}
    override fun removeStack(slot: Int, amount: Int): ItemStack = EMPTY
    override fun removeStack(slot: Int): ItemStack = EMPTY
    override fun setStack(slot: Int, stack: ItemStack) {}

    override fun markDirty() {
        val logger by lazyLogger(this)
        logger.info("{} marked as dirty: {}", this::class.java.simpleName, this)
    }

    override fun onOpen(player: PlayerEntity) {}
    override fun onClose(player: PlayerEntity) {}
    override fun canInsert(slot: Int, stack: ItemStack, dir: Direction?) = false
    override fun canExtract(slot: Int, stack: ItemStack, dir: Direction?) = false
}
