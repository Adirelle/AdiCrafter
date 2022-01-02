package dev.adirelle.adicrafter.utils.callback.api

import dev.adirelle.adicrafter.utils.callback.impl.CallbackImpl

interface Callback<T> : AutoCloseable {

    companion object {

        fun <T> create(): Callback<T> = CallbackImpl()
    }

    operator fun invoke(callback: Subscriber<T>): AutoCloseable

    operator fun invoke(callback: (T) -> Unit) =
        invoke(Subscriber(callback))

    fun trigger(data: T)
}
