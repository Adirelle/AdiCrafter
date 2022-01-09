package dev.adirelle.adicrafter.utils.minecraft

import net.minecraft.nbt.NbtElement
import net.minecraft.nbt.NbtList

fun interface NbtPersistable<E : NbtElement> {

    fun toNbt(): E
}

fun NbtList.set(index: Int, value: NbtPersistable<*>) =
    set(index, value.toNbt())
