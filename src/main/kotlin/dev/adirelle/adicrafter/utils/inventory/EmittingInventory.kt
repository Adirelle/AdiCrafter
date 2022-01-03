package dev.adirelle.adicrafter.utils.inventory

import dev.adirelle.adicrafter.utils.Event
import net.minecraft.inventory.Inventory

interface EmittingInventory : Inventory, Event<EmittingInventory> {

    abstract class Abstract(
        protected val event: Event<EmittingInventory> = Event.Default<EmittingInventory>()
    ) : EmittingInventory, Event<EmittingInventory> by event {

        override fun markDirty() {
            event.send(this)
        }
    }

    class Decorator(protected val backing: Inventory) : EmittingInventory, Abstract(), Inventory by backing {

        override fun markDirty() {
            backing.markDirty()
            super.markDirty()
        }
    }
}
