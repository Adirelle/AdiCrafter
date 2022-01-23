@file:Suppress("UnstableApiUsage")

package dev.adirelle.adicrafter.crafter.impl.power

import dev.adirelle.adicrafter.crafter.api.power.PowerGenerator
import dev.adirelle.adicrafter.crafter.api.power.PowerVariant
import dev.adirelle.adicrafter.utils.Listenable
import dev.adirelle.adicrafter.utils.SimpleListenable
import dev.adirelle.adicrafter.utils.extensions.asStorage
import dev.adirelle.adicrafter.utils.extensions.get
import dev.adirelle.adicrafter.utils.inventory.ListenableInventory
import dev.adirelle.adicrafter.utils.inventory.SimpleListenableInventory
import dev.adirelle.adicrafter.utils.lazyLogger
import dev.adirelle.adicrafter.utils.toItemString
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext
import net.fabricmc.fabric.api.transfer.v1.transaction.base.SnapshotParticipant
import net.fabricmc.fabric.api.util.NbtType
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NbtCompound
import java.util.*
import kotlin.math.min

open class ItemConsumerGenerator(
    private val itemPower: (Item) -> Optional<Long>,
    private val listenable: SimpleListenable = SimpleListenable()
) : PowerGenerator, Listenable by listenable, SnapshotParticipant<Long>() {

    private val logger by lazyLogger

    private val inventory: SimpleListenableInventory =
        object : SimpleListenableInventory(1) {

            override fun isValid(slot: Int, stack: ItemStack) =
                super.isValid(slot, stack) && itemPower(stack.item).isPresent
        }

    private val storage = inventory.asStorage()

    private var stack: ItemStack
        get() = inventory.getStack(0)
        set(value) = inventory.setStack(0, value)

    private var buffer: Long = 0L

    init {
        inventory.addListener(listenable)
    }

    override fun hasPowerBar() = false
    override fun getAmount() = itemPower(stack.item).map { it * storage[0].amount }.orElse(0L)
    override fun getCapacity() = itemPower(stack.item).map { it * storage[0].capacity }.orElse(0L)
    override fun asInventory(): ListenableInventory = inventory

    override fun extract(resource: PowerVariant, maxAmount: Long, tx: TransactionContext): Long {
        if (maxAmount > buffer) {
            consumeItem(maxAmount - buffer, tx)
        }
        val extracted = min(maxAmount, buffer)
        if (extracted > 0L) {
            updateSnapshots(tx)
            buffer -= extracted
        }
        return extracted
    }

    private fun consumeItem(amount: Long, tx: TransactionContext) {
        itemPower(stack.item).ifPresent { powerPerItem ->
            val request = min((amount + powerPerItem - 1) / powerPerItem, storage[0].amount)
            val extracted = storage.extract(storage.slots[0].resource, request, tx)
            if (extracted > 0) {
                updateSnapshots(tx)
                buffer += powerPerItem * extracted
            }
        }
    }

    override fun readFromNbt(nbt: NbtCompound) {
        inventory.readNbtList(nbt.getList("inventory", NbtType.COMPOUND))
        buffer = nbt.getLong("buffer")
        logger.info("ItemConsumerGenerator::readFromNbt, buffer={}, stack={}", buffer, stack.toItemString())
    }

    override fun toNbt() = NbtCompound().apply {
        put("inventory", inventory.toNbtList())
        putLong("buffer", buffer)
        logger.info("ItemConsumerGenerator::toNbt, buffer={}, stack={}", buffer, stack.toItemString())
    }

    override fun createSnapshot() = buffer

    override fun readSnapshot(snapshot: Long) {
        buffer = snapshot
    }

    override fun onFinalCommit() {
        listenable.listen()
    }
}
