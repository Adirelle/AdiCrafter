package dev.adirelle.adicrafter.utils.general

import net.minecraft.nbt.NbtElement
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

open class ValueHolder<T : Any>(initialValue: T) : ReadWriteProperty<Any, T> {

    private val logger by lazyLogger

    private var value: T = initialValue

    fun get(): T = value

    open fun set(newValue: T): Boolean {
        val oldValue = value
        if (areValuesEqual(oldValue, newValue)) return false
        value = newValue
        onValueChanged(oldValue, newValue)
        return true
    }

    open fun <E : NbtElement> readFromNbt(nbt: E, deserialize: (E) -> T, fallback: T) =
        set(internalFromNBT(nbt, deserialize, fallback))

    protected fun <E : NbtElement> internalFromNBT(nbt: E, deserialize: (E) -> T, fallback: T) =
        try {
            deserialize(nbt)
        } catch (t: Throwable) {
            logger.warn("error while reading from NBT", t)
            fallback
        }

    protected open fun areValuesEqual(a: T, b: T): Boolean =
        areEqual(a, b)

    protected open fun onValueChanged(oldValue: T, newValue: T) {
    }

    override fun equals(other: Any?) =
        other is ValueHolder<*> &&
            other.value::class.java == value::class.java &&
            @Suppress("UNCHECKED_CAST")
            areValuesEqual(value, other.value as T)

    override fun hashCode() = value.hashCode()
    override fun toString() = value.toString()

    override fun getValue(thisRef: Any, property: KProperty<*>): T = get()
    override fun setValue(thisRef: Any, property: KProperty<*>, value: T) {
        set(value)
    }
}

open class ObservableValueHolder<T : Any>(initialValue: T) : ValueHolder<T>(initialValue) {

    private val observable = Observable<T>()

    fun observeValue(observer: Observer<T>) =
        observable.addObserver(observer)

    override fun set(newValue: T) =
        set(newValue, true)

    fun set(newValue: T, emit: Boolean) =
        super.set(newValue).also {
            if (it && emit) {
                observable.notify(newValue)
            }
        }

    override fun <E : NbtElement> readFromNbt(nbt: E, deserialize: (E) -> T, fallback: T) =
        set(internalFromNBT(nbt, deserialize, fallback), false)

}
