package dev.adirelle.adicrafter.utils

import dev.adirelle.adicrafter.utils.Listenable.Listener

class SimpleListenable : Listenable, Listener {

    private val listeners = mutableListOf<Listener>()

    fun hasListeners() =
        synchronized(listeners) { listeners.isNotEmpty() }

    override fun addListener(listener: Listener) {
        synchronized(listeners) {
            listeners.add(listener)
        }
    }

    override fun removeListener(listener: Listener) {
        synchronized(listeners) {
            listeners.remove(listener)
        }
    }

    override fun listen() {
        synchronized(listeners) {
            listeners.forEach(Listener::listen)
        }
    }
}
