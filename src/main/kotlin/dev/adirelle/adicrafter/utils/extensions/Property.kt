package dev.adirelle.adicrafter.utils

import net.minecraft.screen.Property
import kotlin.properties.PropertyDelegateProvider
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

fun <T> Property.delegate(): PropertyDelegateProvider<T, ReadWriteProperty<T, Int>> =
    PropertyDelegateProvider { _, _ ->
        object : ReadWriteProperty<T, Int> {
            override fun getValue(thisRef: T, property: KProperty<*>) = this@delegate.get()
            override fun setValue(thisRef: T, property: KProperty<*>, value: Int) {
                this@delegate.set(value)
            }
        }
    }

