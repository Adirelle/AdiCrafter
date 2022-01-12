package dev.adirelle.adicrafter.utils.extensions

import java.util.*
import java.util.function.Predicate
import java.util.stream.Stream
import java.util.stream.StreamSupport

fun <T> Iterator<T>.toStream(characteristics: Int = Spliterator.ORDERED): Stream<T> =
    StreamSupport.stream(Spliterators.spliteratorUnknownSize(this, characteristics), false)

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

fun <T> Iterator<T>.takeUntil(predicate: Predicate<T>): Iterator<T> =
    this.takeWhile(predicate.negate())

fun <T> Iterator<T>.filter(predicate: Predicate<T>): Iterator<T> =
    iterator {
        while (hasNext()) {
            val item = next()
            if (predicate.test(item)) {
                yield(item)
            }
        }
    }
