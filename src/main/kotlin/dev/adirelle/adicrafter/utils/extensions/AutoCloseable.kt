package dev.adirelle.adicrafter.utils.extensions

fun Iterable<AutoCloseable>.close() =
    forEach { it.close() }
