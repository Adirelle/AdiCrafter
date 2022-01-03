package dev.adirelle.adicrafter.utils

import org.apache.logging.log4j.LogManager
import java.util.function.BiFunction
import java.util.function.BiPredicate
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

inline fun <T> onChangeCallback(initialValue: T, crossinline callback: () -> Unit) =
    ChangeObservableProperty(initialValue, { _, _ -> callback() })

inline fun <T> onChangeCallback(
    initialValue: T,
    equalityPredicate: BiPredicate<T, T>,
    crossinline callback: () -> Unit
) =
    ChangeObservableProperty(initialValue, { _, _ -> callback() }, equalityPredicate)

fun <T> onChangeCallback(initialValue: T, callback: (T, T) -> Unit) =
    ChangeObservableProperty(initialValue, callback)

fun <T> onChangeCallback(initialValue: T, equalityTest: BiPredicate<T, T>, callback: (T, T) -> Unit) =
    ChangeObservableProperty(initialValue, callback, equalityTest)

class ChangeObservableProperty<T>(
    private var value: T,
    private val callback: BiFunction<T, T, Unit>,
    private val equalityPredicate: BiPredicate<T, T> = BiPredicate { a, b -> a == b }
) : ReadWriteProperty<Any?, T> {

    override fun getValue(thisRef: Any?, property: KProperty<*>): T = value

    @Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
    override fun setValue(thisRef: Any?, property: KProperty<*>, newValue: T) {
        val oldValue = value
        if (equalityPredicate.test(oldValue, newValue)) {
            return
        }
        value = newValue
        LogManager.getLogger(thisRef).info("{} property changed: {} -> {}", property.name, oldValue, newValue)
        callback.apply(newValue, oldValue)
    }
}
