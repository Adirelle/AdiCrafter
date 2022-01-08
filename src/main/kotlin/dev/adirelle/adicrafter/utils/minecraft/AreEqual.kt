package dev.adirelle.adicrafter.utils

import net.minecraft.item.ItemStack
import net.minecraft.recipe.Recipe
import java.util.*

/**
 * Compare ItemStacks using the static comparator
 */
fun areEqual(a: ItemStack, b: ItemStack) =
    ItemStack.areEqual(a, b)

/**
 * Compare Recipes by type and id
 */
fun areEqual(a: Recipe<*>, b: Recipe<*>) =
    a.type === b.type && a.id === b.id

/**
 * Compare Arrays by size and content
 */
fun <T> areEqual(a: Array<T>, b: Array<T>) =
    a.size == b.size && a.withIndex().all { (i, itemA) -> areEqual(itemA, b[i]) }

/**
 * Compare Iterables by size and content
 */
fun <T> areEqual(a: Iterable<T>, b: Iterable<T>) =
    areEqual(a.iterator(), b.iterator())

/**
 * Compare Iterator by size and content
 */
fun <T> areEqual(a: Iterator<T>, b: Iterator<T>): Boolean {
    while (a.hasNext() && b.hasNext()) {
        if (!areEqual(a.next(), b.next())) {
            return false
        }
    }
    return a.hasNext() == b.hasNext()
}

/**
 * Compare Optionals by presence and content
 */
fun <T : Any> areEqual(a: Optional<T>, b: Optional<T>) =
    a.isPresent == b.isPresent && (!a.isPresent || areEqual(a.get(), b.get()))

/**
 * Generic equality check using isEquals
 */
fun <T> areEqual(a: T, b: T) = a == b

/**
 * Execute the lambda if both items are equal by areEqual rules
 */
inline fun <T> ifEqual(a: T, b: T, block: () -> Unit): Boolean =
    if (areEqual(a, b)) {
        block()
        true
    } else false

/**
 * Execute the lambda if the items are different by areEqual rules
 */
inline fun <T> ifDifferent(a: T, b: T, block: () -> Unit): Boolean =
    if (!areEqual(a, b)) {
        block()
        true
    } else false
