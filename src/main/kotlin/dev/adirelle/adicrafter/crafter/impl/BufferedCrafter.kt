@file:Suppress("UnstableApiUsage")

package dev.adirelle.adicrafter.crafter.impl

import dev.adirelle.adicrafter.crafter.api.Crafter
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext
import net.fabricmc.fabric.api.transfer.v1.transaction.base.SnapshotParticipant
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NbtCompound
import net.minecraft.util.ItemScatterer
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World

class BufferedCrafter(
    private val backing: Crafter,
    private val listener: Crafter.Listener
) : Crafter by backing, SnapshotParticipant<ItemStack>() {

    private var buffer: ItemStack = ItemStack.EMPTY

    override fun onRemoved(world: World, pos: BlockPos) {
        if (buffer.isEmpty) return
        pos.up().let { dropPos ->
            ItemScatterer.spawn(
                world,
                dropPos.x.toDouble(),
                dropPos.y.toDouble(),
                dropPos.z.toDouble(),
                buffer
            )
        }
        buffer = ItemStack.EMPTY
    }

    override fun readFromNbt(nbt: NbtCompound) {
        backing.readFromNbt(nbt)
        buffer = ItemStack.fromNbt(nbt)
    }

    override fun toNbt(): NbtCompound =
        backing.toNbt().also {
            buffer.writeNbt(it)
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
