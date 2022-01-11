package dev.adirelle.adicrafter.utils.extensions

import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReadWriteLock

inline fun <T> Lock.locked(block: () -> T): T {
    lock()
    AutoCloseable(this::unlock).use {
        return block()
    }
}

inline fun <T> ReadWriteLock.readLocked(block: () -> T): T =
    readLock().locked { block() }

inline fun <T> ReadWriteLock.writeLocked(block: () -> T): T =
    writeLock().locked { block() }
