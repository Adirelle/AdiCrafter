@file:Suppress("UnstableApiUsage")

package dev.adirelle.adicrafter.crafter.impl

import dev.adirelle.adicrafter.crafter.NbtKeys
import dev.adirelle.adicrafter.crafter.api.Crafter
import dev.adirelle.adicrafter.utils.extensions.getStack
import dev.adirelle.adicrafter.utils.extensions.set
import dev.adirelle.adicrafter.utils.extensions.toStack
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant
import net.fabricmc.fabric.api.transfer.v1.storage.base.ResourceAmount
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext
import net.fabricmc.fabric.api.transfer.v1.transaction.base.SnapshotParticipant
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NbtCompound
import kotlin.math.max
import kotlin.math.min

class BufferedCrafter(
    private val backing: Crafter,
    private val listener: Crafter.Listener
) : Crafter by backing, SnapshotParticipant<ResourceAmount<ItemVariant>>() {

    private var resource: ItemVariant = ItemVariant.blank()
        set(value) {
            field = value
            if (value.isBlank && _amount != 0L) {
                _amount = 0L
            }
        }

    private var _amount: Long = 0L
        set(value) {
            field = value
            if (value == 0L && !resource.isBlank) {
                resource = ItemVariant.blank()
            }
        }

    override fun getAmount() = max(_amount, backing.amount)

    override fun getDroppedStacks(): List<ItemStack> =
        if (_amount > 0)
            listOf(toStack()).also { _amount = 0L }
        else
            emptyList()

    override fun readFromNbt(nbt: NbtCompound) {
        backing.readFromNbt(nbt)
        val stack = nbt.getStack(NbtKeys.BUFFER)
        resource = ItemVariant.of(stack)
        _amount = stack.count.toLong()
    }

    override fun writeToNbt(nbt: NbtCompound) {
        backing.writeToNbt(nbt)
        nbt[NbtKeys.BUFFER] = toStack()
    }

    override fun extract(resource: ItemVariant, maxAmount: Long, tx: TransactionContext): Long {
        if (maxAmount < 1 || backing.isEmpty || resource != backing.resource) return 0L
        if (maxAmount > _amount) {
            val crafted = backing.extract(resource, maxAmount - _amount, tx)
            if (crafted > 0) {
                updateSnapshots(tx)
                this.resource = resource
                _amount += crafted
            }
        }
        val extracted = min(maxAmount, _amount)
        if (extracted > 0) {
            updateSnapshots(tx)
            _amount -= extracted
        }
        return extracted
    }

    override fun createSnapshot(): ResourceAmount<ItemVariant> =
        ResourceAmount(resource, _amount)

    override fun readSnapshot(snapshot: ResourceAmount<ItemVariant>) {
        resource = snapshot.resource
        _amount = snapshot.amount
    }

    override fun onFinalCommit() {
        listener.onCrafterUpdate()
    }

    override fun insert(resource: ItemVariant, maxAmount: Long, transaction: TransactionContext) =
        super.insert(resource, maxAmount, transaction)
}
