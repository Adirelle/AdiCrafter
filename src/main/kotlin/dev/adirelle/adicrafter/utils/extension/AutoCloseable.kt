package dev.adirelle.adicrafter.utils.extension

fun Iterable<AutoCloseable>.close() =
    forEach { it.close() }
