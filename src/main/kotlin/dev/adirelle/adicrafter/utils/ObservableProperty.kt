package dev.adirelle.adicrafter.utils

import org.apache.logging.log4j.LogManager
import java.util.function.BiPredicate
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

class ObservableProperty<T>(
    private var value: T,
    private val equalityPredicate: BiPredicate<T, T> = BiPredicate { a, b -> a == b },
    private val event: Event<T> = Event.Default<T>()
) : ReadWriteProperty<Any, T>, Event<T> by event {

    override fun getValue(thisRef: Any, property: KProperty<*>): T = value

    @Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
    override fun setValue(thisRef: Any, property: KProperty<*>, newValue: T) {
        val oldValue = value
        if (equalityPredicate.test(oldValue, newValue)) {
            return
        }
        value = newValue
        LogManager.getLogger(thisRef)
            .info("{}.{} property changed: {} -> {}", thisRef.javaClass.simpleName, property.name, oldValue, newValue)
        event.send(value)
    }
}
