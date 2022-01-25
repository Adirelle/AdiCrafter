@file:Suppress("UnstableApiUsage")

package dev.adirelle.adicrafter.crafter.impl

import dev.adirelle.adicrafter.crafter.NbtKeys
import dev.adirelle.adicrafter.crafter.api.Crafter
import dev.adirelle.adicrafter.utils.extensions.getStack
import dev.adirelle.adicrafter.utils.extensions.set
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext
import net.fabricmc.fabric.api.transfer.v1.transaction.base.SnapshotParticipant
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NbtCompound

class BufferedCrafter(
    private val backing: Crafter,
    private val listener: Crafter.Listener
) : Crafter by backing, SnapshotParticipant<ItemStack>() {

    private var buffer: ItemStack = ItemStack.EMPTY

    override fun getDroppedStacks(): List<ItemStack> =
        if (buffer.isEmpty)
            emptyList()
        else
            listOf(buffer).also {
                buffer = ItemStack.EMPTY
            }

    override fun readFromNbt(nbt: NbtCompound) {
        backing.readFromNbt(nbt)
        buffer = nbt.getStack(NbtKeys.BUFFER)
    }

    override fun writeToNbt(nbt: NbtCompound) {
        backing.writeToNbt(nbt)
        nbt[NbtKeys.BUFFER] = buffer
    }

    override fun getAmount() = buffer.count.toLong()

    override fun extract(resource: ItemVariant, maxAmount: Long, tx: TransactionContext): Long {
        if (maxAmount < 1 || backing.isEmpty || resource != backing.resource) return 0L
        updateSnapshots(tx)
        val current = buffer.count.toLong()
        if (maxAmount > current) {
            val crafted = backing.extract(resource, maxAmount - current, tx)
            if (buffer.isEmpty) {
                buffer = backing.resource.toStack(crafted.toInt())
            } else {
                require(backing.resource.matches(buffer)) { "crafter/buffer mismatch" }
                buffer.increment(crafted.toInt())
            }
        }
        val extracted = buffer.split(maxAmount.toInt())
        return extracted.count.toLong()
    }

    override fun createSnapshot(): ItemStack =
        buffer.copy()

    override fun readSnapshot(snapshot: ItemStack) {
        buffer = snapshot
    }

    override fun onFinalCommit() {
        listener.onCrafterUpdate()
    }

    override fun insert(resource: ItemVariant, maxAmount: Long, transaction: TransactionContext) =
        super.insert(resource, maxAmount, transaction)
}
