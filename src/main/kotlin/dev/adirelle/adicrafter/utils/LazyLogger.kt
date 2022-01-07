package dev.adirelle.adicrafter.utils

import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import kotlin.reflect.KProperty

class LazyLogger(private val name: Any?) {

    private var logger: Logger? = null

    operator fun <T> getValue(thisRef: T, property: KProperty<*>): Logger {
        if (logger == null) {
            logger =
                if (name is String) LogManager.getLogger(name)
                else LogManager.getLogger(name ?: thisRef)
        }
        return logger!!
    }
}

fun lazyLogger(name: Any? = null) = LazyLogger(name)
