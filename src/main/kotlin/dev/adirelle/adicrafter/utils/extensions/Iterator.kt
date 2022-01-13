package dev.adirelle.adicrafter.utils.extensions

import java.util.function.Predicate

fun <T> Iterator<T>.takeWhile(predicate: Predicate<T>): Iterator<T> =
    iterator {
        while (hasNext()) {
            val item = next()
            if (predicate.test(item)) {
                yield(item)
            } else {
                break
            }
        }
    }
