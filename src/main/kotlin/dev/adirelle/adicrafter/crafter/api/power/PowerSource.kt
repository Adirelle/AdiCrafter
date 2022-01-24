@file:Suppress("UnstableApiUsage")

package dev.adirelle.adicrafter.crafter.api.power

import dev.adirelle.adicrafter.utils.Droppable
import dev.adirelle.adicrafter.utils.NbtSerializable
import dev.adirelle.adicrafter.utils.Tickable
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView
import net.minecraft.inventory.Inventory

interface PowerSource : StorageView<PowerVariant>, Tickable, NbtSerializable, Droppable {

    fun asInventory(): Inventory? = null
    fun hasPowerBar(): Boolean = false

    override fun isResourceBlank() = amount == 0L
    override fun getResource() = PowerVariant

    fun interface Listener {

        fun onPowerChanged()
    }
}
