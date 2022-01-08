package dev.adirelle.adicrafter.utils

import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.recipe.Recipe
import java.util.*

fun areEqual(a: Number, b: Number) =
    a == b

fun areEqual(a: String, b: String) =
    a == b

fun areEqual(a: Item, b: Item) =
    a == b

fun areEqual(a: ItemStack, b: ItemStack) =
    ItemStack.areEqual(a, b)

fun areEqual(a: Recipe<*>, b: Recipe<*>) =
    a.type === b.type && a.id === b.id

fun <T> areEqual(a: Array<T>, b: Array<T>) =
    a.size == b.size && a.withIndex().all { (i, itemA) -> areEqual(itemA, b[i]) }

fun <T> areEqual(a: Iterable<T>, b: Iterable<T>) =
    areEqual(a.iterator(), b.iterator())

fun <T> areEqual(a: Iterator<T>, b: Iterator<T>): Boolean {
    while (a.hasNext() && b.hasNext()) {
        if (!areEqual(a.next(), b.next())) {
            return false
        }
    }
    return a.hasNext() == b.hasNext()
}

fun <T : Any> areEqual(a: Optional<T>, b: Optional<T>) =
    a.isPresent == b.isPresent && (!a.isPresent || areEqual(a.get(), b.get()))

fun <T> areEqual(a: T, b: T) = a == b

inline fun <T> ifEqual(a: T, b: T, block: () -> Unit): Boolean =
    if (areEqual(a, b)) {
        block()
        true
    } else false

inline fun <T> ifDifferent(a: T, b: T, block: () -> Unit): Boolean =
    if (!areEqual(a, b)) {
        block()
        true
    } else false
