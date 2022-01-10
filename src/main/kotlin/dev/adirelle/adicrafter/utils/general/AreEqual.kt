package dev.adirelle.adicrafter.utils.general

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
 * Generic equality check using isEquals
 */
fun <T> areEqual(a: T, b: T) = a == b
