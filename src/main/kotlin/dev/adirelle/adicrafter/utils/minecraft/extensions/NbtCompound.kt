package dev.adirelle.adicrafter.utils.extension

import net.fabricmc.fabric.api.util.NbtType
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NbtCompound

fun NbtCompound.putItemStack(key: String, stack: ItemStack) =
    put(key, stack.toNbt())

fun NbtCompound.putItemStacks(key: String, stacks: Iterable<ItemStack>) =
    put(key, stacks.toNbt())

fun NbtCompound.getItemStack(key: String): ItemStack =
    ItemStack.fromNbt(getCompound(key))

fun NbtCompound.getItemStacks(key: String): List<ItemStack> =
    getList(key, NbtType.COMPOUND)
        .filterIsInstance(NbtCompound::class.java)
        .map(ItemStack::fromNbt)
