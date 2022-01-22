package dev.adirelle.adicrafter.utils

import net.minecraft.nbt.NbtCompound

interface NbtSerializable {

    fun readFromNbt(nbt: NbtCompound)
    fun toNbt(): NbtCompound
}
