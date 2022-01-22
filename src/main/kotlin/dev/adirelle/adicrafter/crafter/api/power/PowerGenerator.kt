@file:Suppress("UnstableApiUsage")

package dev.adirelle.adicrafter.crafter.api.power

import dev.adirelle.adicrafter.crafter.api.power.PowerVariant.INSTANCE
import dev.adirelle.adicrafter.utils.Listenable
import dev.adirelle.adicrafter.utils.NbtSerializable
import dev.adirelle.adicrafter.utils.Tickable
import dev.adirelle.adicrafter.utils.inventory.ListenableInventory
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView
import net.minecraft.nbt.NbtCompound
import net.minecraft.world.World

interface PowerGenerator : StorageView<PowerVariant>, Tickable, Listenable, NbtSerializable {

    fun asInventory(): ListenableInventory? = null

    override fun isResourceBlank() = amount > 0
    override fun getResource() = INSTANCE
    fun hasPowerBar(): Boolean = false
    override fun tick(world: World) = false
    override fun readFromNbt(nbt: NbtCompound) {}
    override fun toNbt(): NbtCompound = NbtCompound()
}
