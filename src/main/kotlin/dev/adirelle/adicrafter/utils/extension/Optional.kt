package dev.adirelle.adicrafter.utils.extension

import java.util.*

inline fun <T : Any, R : Any> Optional<T?>.mapNotNull(crossinline mapper: (T) -> R?): Optional<R> =
    flatMap { value -> if (value == null) Optional.empty() else Optional.ofNullable(mapper(value)) }

inline fun <reified T : Any> Optional<T?>.notNull(): Optional<T> =
    flatMap { Optional.ofNullable(it) }

inline fun <reified T : Any> Optional<*>.instanceOf(): Optional<T> =
    flatMap { Optional.ofNullable(it as? T) }

inline fun <T> Optional<T>.tap(crossinline block: (T) -> Unit): Optional<T> =
    this.also { opt -> opt.map { block(it) } }
