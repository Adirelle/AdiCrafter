package dev.adirelle.adicrafter.utils

import dev.adirelle.adicrafter.utils.extension.readLocked
import dev.adirelle.adicrafter.utils.extension.writeLocked
import java.util.concurrent.locks.ReentrantReadWriteLock

fun interface Listener<in T> {

    fun onBroadcast(message: T)
}

class Broadcaster<T> : Listener<T> {

    private val listeners = ArrayList<Listener<T>>()
    private val lock = ReentrantReadWriteLock()

    fun hasListeners(): Boolean = !listeners.isEmpty()

    fun addListener(listener: Listener<T>): AutoCloseable {
        lock.writeLocked {
            listeners.add(listener)
        }
        return AutoCloseable {
            lock.writeLocked {
                listeners.remove(listener)
            }
        }
    }

    fun emit(message: T) {
        lock.readLocked {
            listeners.forEach { it.onBroadcast(message) }
        }
    }

    override fun onBroadcast(message: T) {
        emit(message)
    }
}
