package dev.adirelle.adicrafter.utils.extension

import org.apache.logging.log4j.LogManager

fun AutoCloseable.safeClose() =
    try {
        close()
    } catch (t: Throwable) {
        val logger by lazy { LogManager.getLogger(this) }
        logger.warn("caught during closing of {}", this::class.java, t)
    }

fun Iterable<AutoCloseable>.close() =
    forEach { it.close() }

fun Iterable<AutoCloseable>.safeClose() =
    forEach { it.safeClose() }
