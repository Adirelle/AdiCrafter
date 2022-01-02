package dev.adirelle.adicrafter.utils.extension

import net.minecraft.nbt.NbtCompound
import net.minecraft.util.Identifier

fun NbtCompound.putIdentifier(key: String, id: Identifier) {
    putString(key, id.path)
}
