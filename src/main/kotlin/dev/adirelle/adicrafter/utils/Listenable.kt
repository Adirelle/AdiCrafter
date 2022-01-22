package dev.adirelle.adicrafter.utils

interface Listenable {

    fun addListener(listener: Listener)
    fun removeListener(listener: Listener)

    fun interface Listener {

        fun listen()
    }

}
