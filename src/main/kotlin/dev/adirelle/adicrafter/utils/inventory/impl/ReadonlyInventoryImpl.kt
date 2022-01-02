package dev.adirelle.adicrafter.utils.inventory.impl

import dev.adirelle.adicrafter.utils.inventory.api.ReadonlyInventory
import dev.adirelle.adicrafter.utils.storage.slotarray.api.VersionFactory
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.inventory.Inventory
import net.minecraft.item.ItemStack
import net.minecraft.util.math.Direction

class ReadonlyInventoryImpl(
    private val backing: Inventory
) : ReadonlyInventory {

    private val availableSlots: IntArray by lazy { IntArray(backing.size()) { it } }

    private val versionFactory = VersionFactory.create()
    override fun getVersion() = versionFactory.value
    override val onContentChanged by versionFactory::onChanged

    override fun size() = backing.size()
    override fun isEmpty() = backing.isEmpty
    override fun getStack(slot: Int): ItemStack = backing.getStack(slot).copy()
    override fun canPlayerUse(player: PlayerEntity?) = backing.canPlayerUse(player)
    override fun getAvailableSlots(side: Direction?): IntArray = availableSlots

    override fun toString() = "RO:{}".format(backing)
}
