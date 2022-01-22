package dev.adirelle.adicrafter.utils.inventory

import dev.adirelle.adicrafter.utils.Listenable.Listener
import dev.adirelle.adicrafter.utils.memoize
import net.minecraft.inventory.InventoryChangedListener
import net.minecraft.inventory.SimpleInventory

open class SimpleListenableInventory(size: Int) : SimpleInventory(size), ListenableInventory {

    private val wrappers = memoize<Listener, InventoryChangedListener> { listener ->
        InventoryChangedListener { listener.listen() }
    }

    override fun addListener(listener: Listener) {
        super.addListener(wrappers(listener))
    }

    override fun removeListener(listener: Listener) {
        wrappers.getOrNull(listener)?.let { super.removeListener(it) }
    }
}

