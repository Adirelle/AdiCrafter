package dev.adirelle.adicrafter.utils

import net.minecraft.inventory.Inventory
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NbtCompound
import net.minecraft.nbt.NbtList
import net.minecraft.recipe.Recipe
import net.minecraft.recipe.RecipeManager
import net.minecraft.recipe.RecipeType
import net.minecraft.util.Identifier
import net.minecraft.util.collection.DefaultedList
import java.lang.Integer.min
import java.util.*

fun Inventory.iterator(): Iterator<ItemStack> {
    val size = size()
    return object : Iterator<ItemStack> {
        var current = 0
        override fun hasNext() = current < size
        override fun next() = getStack(current++)
    }
}

fun Inventory.iteratorWithIndex(): Iterator<IndexedValue<ItemStack>> {
    val size = size()
    return object : Iterator<IndexedValue<ItemStack>> {
        var current = 0
        override fun hasNext() = current < size
        override fun next(): IndexedValue<ItemStack> {
            val index = current++
            return IndexedValue(index, getStack(index))
        }
    }
}

fun Inventory.copyFrom(other: Inventory) {
    val commonSize = min(size(), other.size())
    for (slot in 0 until commonSize) {
        setStack(slot, other.getStack(slot))
    }
}

@Suppress("UNCHECKED_CAST")
fun <T, I> RecipeManager.getOfType(id: Identifier, type: RecipeType<T>): Optional<T>
    where T : Recipe<I>, I : Inventory =
    get(id).filter { it.type == type } as Optional<T>


fun DefaultedList<ItemStack>.toNbt() =
    NbtList().let { result ->
        this.forEachIndexed { slot, stack ->
            if (!stack.isEmpty) {
                result.add(NbtCompound().apply {
                    putInt("Slot", 1 + slot)
                    stack.writeNbt(this)
                })
            }
        }
        result
    }

fun DefaultedList<ItemStack>.readNbt(nbt: NbtList) {
    fill(ItemStack.EMPTY)
    val range = 0 until size
    nbt.filterIsInstance(NbtCompound::class.java).forEach { stackNbt ->
        val slot = stackNbt.getInt("Slot") - 1
        if (slot in range) {
            set(slot, ItemStack.fromNbt(stackNbt))
        }
    }
}
