package dev.adirelle.adicrafter.utils.storage.slotarray.api

import dev.adirelle.adicrafter.utils.callback.api.Callback
import dev.adirelle.adicrafter.utils.storage.slotarray.impl.VersionFactorImpl

typealias Version = Long

interface VersionFactory {

    companion object {

        fun create() = VersionFactorImpl()
    }

    val value: Version

    val onChanged: Callback<Version>

    fun next(): Version
}
