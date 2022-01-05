package dev.adirelle.adicrafter.utils.extension

fun Array<AutoCloseable>.close() =
    forEach { it.close() }
