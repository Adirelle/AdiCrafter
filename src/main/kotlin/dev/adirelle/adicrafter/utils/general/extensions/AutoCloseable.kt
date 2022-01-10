package dev.adirelle.adicrafter.utils.general.extensions

fun Iterable<AutoCloseable>.close() =
    forEach { it.close() }
