@file:Suppress("UnstableApiUsage")

package dev.adirelle.adicrafter.crafter.api.power

import dev.adirelle.adicrafter.utils.storage.SingleViewStorage
import net.minecraft.nbt.NbtCompound
import net.minecraft.world.World

interface PowerGenerator : SingleViewStorage<PowerVariant> {

    fun readFromNbt(nbt: NbtCompound) {}
    fun writeToNbt(nbt: NbtCompound) {}
    fun isActive(): Boolean = true
    fun tick(world: World): Boolean = false
    fun setUpdateCallback(callback: () -> Unit) {}
}
