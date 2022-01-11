package dev.adirelle.adicrafter.utils

import net.minecraft.nbt.NbtElement
import java.util.function.BiConsumer
import java.util.function.BiPredicate
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

fun <T : Any> areEqualBiPredicate(): BiPredicate<T, T> = BiPredicate { a, b -> areEqual(a, b) }
fun <T : Any> onValueChangedNothing(): BiConsumer<T, T> = BiConsumer { _, _ -> }

interface ValueHolder<T : Any> : ReadWriteProperty<Any, T> {

    fun get(): T
    fun set(newValue: T): Boolean

    override fun getValue(thisRef: Any, property: KProperty<*>): T = get()
    override fun setValue(thisRef: Any, property: KProperty<*>, value: T) {
        set(value)
    }
}

open class SimpleValueHolder<T : Any>(
    initialValue: T,
    private val onValueChanged: BiConsumer<T, T> = onValueChangedNothing(),
    private val areValuesEqual: BiPredicate<T, T> = areEqualBiPredicate()
) : ValueHolder<T> {

    private val logger by lazyLogger

    private var value: T = initialValue

    override fun get(): T = value

    override fun set(newValue: T): Boolean {
        val oldValue = value
        if (areValuesEqual.test(oldValue, newValue)) return false
        value = newValue
        onValueChanged.accept(oldValue, newValue)
        return true
    }

    open fun <E : NbtElement> readFromNbt(nbt: E, fallback: T, deserialize: (E) -> T) =
        set(safeFromNBT(nbt, deserialize, fallback))

    protected fun <E : NbtElement> safeFromNBT(nbt: E, deserialize: (E) -> T, fallback: T) =
        try {
            deserialize(nbt)
        } catch (t: Throwable) {
            logger.warn("error while reading from NBT", t)
            fallback
        }

    override fun getValue(thisRef: Any, property: KProperty<*>): T = get()
    override fun setValue(thisRef: Any, property: KProperty<*>, value: T) {
        set(value)
    }
}

class ObservableValueHolder<T : Any>(
    initialValue: T,
    onValueChanged: BiConsumer<T, T> = onValueChangedNothing(),
    areValuesEqual: BiPredicate<T, T> = areEqualBiPredicate(),
    private val observable: Observable<T> = Observable.create()
) : SimpleValueHolder<T>(initialValue, onValueChanged, areValuesEqual),
    Observable<T> by observable {

    override fun set(newValue: T) =
        set(newValue, true)

    fun set(newValue: T, emit: Boolean) =
        super.set(newValue).also {
            if (it && emit) {
                observable.notify(newValue)
            }
        }

    override fun <E : NbtElement> readFromNbt(nbt: E, fallback: T, deserialize: (E) -> T) =
        set(safeFromNBT(nbt, deserialize, fallback), false)
}
