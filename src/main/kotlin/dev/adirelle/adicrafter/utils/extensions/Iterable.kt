package dev.adirelle.adicrafter.utils.extensions

import java.util.*
import java.util.function.Predicate
import java.util.stream.Stream

fun <T> Iterable<T>.takeWhile(predicate: Predicate<T>): Iterator<T> =
    iterator().takeWhile(predicate)

fun <T> Iterable<T>.takeUntil(predicate: Predicate<T>): Iterator<T> =
    iterator().takeUntil(predicate)

fun <T> Iterable<T>.toStream(characteristics: Int = Spliterator.ORDERED): Stream<T> =
    iterator().toStream(characteristics)
