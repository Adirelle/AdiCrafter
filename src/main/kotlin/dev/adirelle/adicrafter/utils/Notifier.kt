package dev.adirelle.adicrafter.utils

fun interface Listener<T> {

    fun onNotify(value: T)
}

interface Notifier<T> {

    fun notify(value: T)

    fun addListener(listener: Listener<T>): AutoCloseable
}

open class DefaultNotifier<T> : Notifier<T> {

    private val listeners = ArrayList<Listener<T>>()

    override fun notify(value: T) {
        listeners.forEach { it.onNotify(value) }
    }

    override fun addListener(listener: Listener<T>): AutoCloseable {
        listeners.add(listener)
        return AutoCloseable { listeners.remove(listener) }
    }
}

