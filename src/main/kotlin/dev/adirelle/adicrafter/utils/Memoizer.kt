package dev.adirelle.adicrafter.utils

import java.lang.ref.WeakReference
import java.util.*

class Memoizer<A : Any, B : Any>(private val computation: (A) -> B) {

    private val instances = WeakHashMap<A, WeakReference<B>>()

    operator fun get(input: A): B {
        synchronized(instances) {
            var instance = instances[input]?.get()
            if (instance == null) {
                instance = computation(input)
                instances[input] = WeakReference(instance)
            }
            return instance
        }
    }

    operator fun invoke(input: A): B = get(input)
}

inline fun <A : Any, B : Any> memoize(crossinline computation: (A) -> B) =
    Memoizer<A, B> { computation(it) }
