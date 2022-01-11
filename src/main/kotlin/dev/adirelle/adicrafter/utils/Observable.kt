package dev.adirelle.adicrafter.utils

import dev.adirelle.adicrafter.utils.extensions.readLocked
import dev.adirelle.adicrafter.utils.extensions.writeLocked
import java.util.concurrent.locks.ReentrantReadWriteLock

fun interface Observer<in T> {

    fun notify(message: T)
}

interface Observable<T> : Observer<T> {

    fun addObserver(observer: Observer<T>): AutoCloseable

    companion object {

        fun <T> create(): Observable<T> =
            SimpleObservable<T>()
    }
}

class SimpleObservable<T> : Observable<T> {

    private val observers = ArrayList<Observer<T>>()
    private val lock = ReentrantReadWriteLock()

    override fun addObserver(observer: Observer<T>): AutoCloseable {
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
