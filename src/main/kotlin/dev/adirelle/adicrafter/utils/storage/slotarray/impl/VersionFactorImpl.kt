package dev.adirelle.adicrafter.utils.storage.slotarray.impl

import dev.adirelle.adicrafter.utils.callback.api.Callback
import dev.adirelle.adicrafter.utils.storage.slotarray.api.Version
import dev.adirelle.adicrafter.utils.storage.slotarray.api.VersionFactory
import java.util.concurrent.atomic.AtomicLong

class VersionFactorImpl : VersionFactory {

    private val atomic = AtomicLong()

    override val value: Version
        get() = atomic.get()

    override val onChanged = Callback.create<Version>()

    override fun next() =
        atomic.incrementAndGet()
            .also { onChanged.trigger(it) }

    override fun toString() = "#$value"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as VersionFactorImpl

        if (value != other.value) return false

        return true
    }

    override fun hashCode() = value.hashCode()
}
