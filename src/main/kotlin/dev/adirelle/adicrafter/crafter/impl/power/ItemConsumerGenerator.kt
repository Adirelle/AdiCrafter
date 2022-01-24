@file:Suppress("UnstableApiUsage")

package dev.adirelle.adicrafter.crafter.impl.power

import dev.adirelle.adicrafter.crafter.api.power.PowerSource
import dev.adirelle.adicrafter.crafter.api.power.PowerSource.Listener
import dev.adirelle.adicrafter.crafter.api.power.PowerVariant
import dev.adirelle.adicrafter.utils.extensions.asStorage
import dev.adirelle.adicrafter.utils.extensions.get
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext
import net.fabricmc.fabric.api.transfer.v1.transaction.base.SnapshotParticipant
import net.fabricmc.fabric.api.util.NbtType
import net.minecraft.inventory.SimpleInventory
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NbtCompound
import kotlin.math.max
import kotlin.math.min

open class ItemConsumerGenerator(
    private val powerPerItem: Map<Item, Long>,
    private val listener: Listener
) : PowerSource, SnapshotParticipant<Long>() {

    private val maxPowerPerItem = powerPerItem.values.maxOf { it }

    private val inventory: SimpleInventory =
        object : SimpleInventory(1) {

            override fun isValid(slot: Int, stack: ItemStack) =
                super.isValid(slot, stack) && stack.item in powerPerItem

            override fun markDirty() {
                if (!Transaction.isOpen()) {
                    listener.onPowerChanged()
                }
            }
        }

    private val storage = inventory.asStorage()

    private var stack: ItemStack
        get() = inventory.getStack(0)
        set(value) = inventory.setStack(0, value)

    private var buffer: Long = 0L

    override fun hasPowerBar() = true
    override fun getAmount() = max(buffer, powerPerItem[stack.item] ?: 0L)
    override fun getCapacity() = maxPowerPerItem
    override fun asInventory() = inventory

    override fun extract(resource: PowerVariant, maxAmount: Long, tx: TransactionContext): Long {
        if (maxAmount > buffer) {
            val consumed = consumeItem(maxAmount - buffer, tx)
            if (consumed > 0) {
                updateSnapshots(tx)
                buffer += consumed
            }
        }
        val extracted = min(maxAmount, buffer)
        if (extracted > 0L) {
            updateSnapshots(tx)
            buffer -= extracted
        }
        return extracted
    }

    private fun consumeItem(amount: Long, tx: TransactionContext): Long {
        val powerPerItem = powerPerItem[stack.item] ?: return 0
        val request = min((amount + powerPerItem - 1) / powerPerItem, storage[0].amount)
        return powerPerItem * storage.extract(storage.slots[0].resource, request, tx)
    }

    override fun readFromNbt(nbt: NbtCompound) {
        inventory.readNbtList(nbt.getList("inventory", NbtType.COMPOUND))
        buffer = min(nbt.getLong("buffer"), amount)
    }

    override fun toNbt() = NbtCompound().apply {
        put("inventory", inventory.toNbtList())
        putLong("buffer", buffer)
    }

    override fun createSnapshot() = buffer

    override fun readSnapshot(snapshot: Long) {
        buffer = snapshot
    }

    override fun onFinalCommit() {
        listener.onPowerChanged()
    }
}
