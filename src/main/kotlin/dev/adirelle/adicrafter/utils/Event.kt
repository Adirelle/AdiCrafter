package dev.adirelle.adicrafter.utils

interface Event<E> {

    fun listen(listener: Listener<E>): AutoCloseable

    fun send(data: E)

    fun interface Listener<E> {

        fun onReceive(data: E)
    }

    class Default<E> : Event<E> {

        private val logger by lazyLogger()

        private val listeners = ArrayList<Listener<E>>()

        override fun listen(listener: Listener<E>): AutoCloseable {
            listeners.add(listener)
            logger.info("registered {}", listener)
            return AutoCloseable {
                listeners.remove(listener)
                logger.info("unregistered {}", listener)
            }
        }

        override fun send(data: E) {
            logger.info("sending {}", data)
            listeners.forEach { it.onReceive(data) }
        }
    }

}
