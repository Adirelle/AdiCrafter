package dev.adirelle.adicrafter.utils

import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

class ObservableProperty<T>(initialValue: T, private val notifier: Notifier<T>) : ReadWriteProperty<Any, T>,
                                                                                  Notifier<T> by notifier {

    private var current = initialValue

    override fun getValue(thisRef: Any, property: KProperty<*>): T = current

    override fun setValue(thisRef: Any, property: KProperty<*>, value: T) {
        if (value != current) {
            current = value
            notify(value)
        }
    }
}

fun <T> observable(initialValue: T) =
    ObservableProperty(initialValue, DefaultNotifier())

fun <T> observable(initialValue: T, notifier: Notifier<T>) =
    ObservableProperty(initialValue, notifier)
