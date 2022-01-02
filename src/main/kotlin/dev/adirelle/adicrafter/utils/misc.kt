package dev.adirelle.adicrafter.utils

import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import kotlin.reflect.KProperty

fun lazyLogger(name: Any? = null) = LazyLogger(name)

class LazyLogger(private val name: Any?) {

    operator fun <T> getValue(thisRef: T, property: KProperty<*>): Logger {
        val logger by lazy {
            if (name is String) LogManager.getLogger(name)
            else LogManager.getLogger(name ?: thisRef)
        }
        return logger
    }
}
