package dev.adirelle.adicrafter.utils

import java.util.*
import java.util.concurrent.atomic.AtomicInteger

interface Emitter<T : Any> {

    fun emit(payload: T)

    fun listen(receiver: Receiver<T>): AutoCloseable
}

fun interface Receiver<in T : Any> {

    fun onReceived(payload: T)
}

open class BaseEmitter<T : Any> : Emitter<T> {

    private val logger by lazyLogger()

    private var receivers = Collections.synchronizedMap(WeakHashMap<Receiver<T>, AutoCloseable>())

    override fun listen(receiver: Receiver<T>): AutoCloseable {
        val subscription = receivers.computeIfAbsent(receiver) {
            AutoCloseable { receivers.remove(receiver) }
        }
        onNewReceiver(receiver)
        return subscription
    }

    open fun onNewReceiver(receiver: Receiver<T>) {}

    override fun emit(payload: T) {
        synchronized(receivers) {
            receivers.keys.forEach { it.onReceived(payload) }
        }
    }
}

class NullEmitter<T : Any> : Emitter<T> {

    private fun closeNothing() {}
    override fun emit(payload: T) {}
    override fun listen(receiver: Receiver<T>) =
        AutoCloseable(this::closeNothing)
}

class DebounceEmitter<T : Any>(private val backing: Emitter<T>) : Emitter<T> by backing

class LockableEmitter<T : Any>(private val backing: Emitter<T>) : Emitter<T> by backing {

    private val logger by lazyLogger()

    private val lockLevel = AtomicInteger(0)

    override fun emit(payload: T) {
        if (lockLevel.get() == 0) {
            backing.emit(payload)
        }
    }

    fun lock(): AutoCloseable {
        val level = lockLevel.incrementAndGet()
        return AutoCloseable {
            if (!lockLevel.compareAndSet(level, level - 1)) {
                logger.warn("uneven lock/releasing of LockableEmitter")
            }
        }
    }
}
