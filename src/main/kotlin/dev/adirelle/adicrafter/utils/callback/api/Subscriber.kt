package dev.adirelle.adicrafter.utils.callback.api

fun interface Subscriber<in T> {

    operator fun invoke(data: T)
}
