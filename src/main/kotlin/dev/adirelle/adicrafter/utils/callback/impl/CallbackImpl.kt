package dev.adirelle.adicrafter.utils.callback.impl

import dev.adirelle.adicrafter.utils.callback.api.Callback
import dev.adirelle.adicrafter.utils.callback.api.Subscriber
import dev.adirelle.adicrafter.utils.lazyLogger
import java.lang.ref.WeakReference
import java.util.*

class CallbackImpl<T> : Callback<T> {

    val logger by lazyLogger()

    private var callbacks = WeakHashMap<Subscriber<T>, Subscription<T>>()

    private var closed = false

    private val weakThis by lazy { WeakReference(this) }

    private fun assertNotClosed() {
        if (synchronized(closed) { closed }) {
            throw IllegalStateException("unsupported operation on a closed callback")
        }
    }

    override fun invoke(callback: Subscriber<T>): AutoCloseable {
        assertNotClosed()
        return synchronized(callbacks) {
            callbacks.computeIfAbsent(callback) {
                logger.debug("[{}] subscribing {}", this, it)
                Subscription(weakThis, it)
            }
        }
    }

    internal fun remove(key: Subscriber<T>) {
        return synchronized(callbacks) {
            if (callbacks.remove(key) != null) {
                logger.debug("[{}] unsubscribed {}", this, key)
            }
        }
    }

    override fun trigger(data: T) {
        assertNotClosed()
        synchronized(callbacks) {
            logger.debug("[{}] triggered with {}", this, data)
            callbacks.keys.forEach { it: Subscriber<T> -> it(data) }
        }
    }

    override fun close() {
        synchronized(closed) {
            if (closed) return
            closed = true
        }
        synchronized(callbacks) {
            callbacks.clear()
        }
        logger.debug("[{}] closed", this)
    }

    private class Subscription<T>(thisRef: WeakReference<CallbackImpl<T>>, key: Subscriber<T>) : AutoCloseable {

        private val done = lazy { thisRef.get()?.remove(key) }

        override fun close() {
            done.value
        }
    }
}
