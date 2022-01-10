package dev.adirelle.adicrafter.utils.general

import dev.adirelle.adicrafter.utils.general.extensions.readLocked
import dev.adirelle.adicrafter.utils.general.extensions.writeLocked
import java.util.concurrent.locks.ReentrantReadWriteLock

fun interface Observer<in T> {

    fun notify(message: T)
}

class Observable<T> : Observer<T> {

    private val observers = ArrayList<Observer<T>>()
    private val lock = ReentrantReadWriteLock()

    fun addObserver(observer: Observer<T>): AutoCloseable {
        lock.writeLocked {
            observers.add(observer)
        }
        return AutoCloseable {
            lock.writeLocked {
                observers.remove(observer)
            }
        }
    }

    override fun notify(message: T) {
        lock.readLocked {
            observers.forEach { it.notify(message) }
        }
    }
}
