package dev.adirelle.adicrafter.utils

import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

class BoxedProperty<T>(
    val get: () -> T,
    val set: (T) -> Unit,
    val areEqual: (T, T) -> Boolean = { a, b -> a == b }
) : ReadWriteProperty<Any, T> {

    inline fun <R> let(block: (T) -> R): R =
        block(get())

    inline fun update(block: (T) -> T): T =
        block(get()).also { set(it) }

    inline fun changeAnd(newValue: T, block: (T) -> Unit) =
        changeAnd(newValue, Unit) { block(it) }

    inline fun <R> changeAnd(newValue: T, elseValue: R, block: (T) -> R): R {
        val oldValue = get()
        return if (!areEqual(oldValue, newValue)) {
            set(newValue)
            block(newValue)
        } else elseValue
    }

    override fun getValue(thisRef: Any, property: KProperty<*>) =
        get()

    override fun setValue(thisRef: Any, property: KProperty<*>, value: T) =
        set(value)
}
