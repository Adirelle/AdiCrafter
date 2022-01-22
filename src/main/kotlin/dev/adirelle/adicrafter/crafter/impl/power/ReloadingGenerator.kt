@file:Suppress("UnstableApiUsage")

package dev.adirelle.adicrafter.crafter.impl.power

import dev.adirelle.adicrafter.crafter.api.power.PowerGenerator
import dev.adirelle.adicrafter.crafter.api.power.PowerVariant
import dev.adirelle.adicrafter.crafter.api.power.PowerVariant.INSTANCE
import dev.adirelle.adicrafter.utils.Listenable
import dev.adirelle.adicrafter.utils.SimpleListenable
import dev.adirelle.adicrafter.utils.withOuterTransaction
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext
import net.fabricmc.fabric.api.transfer.v1.transaction.base.SnapshotParticipant
import net.minecraft.nbt.NbtCompound
import net.minecraft.world.World
import kotlin.math.min

open class ReloadingGenerator(
    private val _capacity: Long,
    private val reloadRate: Long,
    private val source: PowerGenerator,
    private val listenable: SimpleListenable = SimpleListenable()
) : PowerGenerator, SnapshotParticipant<Long>(), Listenable by listenable {

    init {
        source.addListener { listenable.listen() }
    }

    private var _amount = 0L

    override fun hasPowerBar() = true
    override fun getCapacity() = _capacity
    override fun getAmount() = _amount
    override fun asInventory() = source.asInventory()

    override fun extract(resource: PowerVariant, maxAmount: Long, tx: TransactionContext): Long {
        val extracted = min(maxAmount, _amount)
        if (extracted > 0) {
            updateSnapshots(tx)
            _amount -= extracted
        }
        return extracted
    }

    override fun tick(world: World): Boolean {
        val sourceChanged = source.tick(world)
        return tickInternal() || sourceChanged
    }

    private fun tickInternal(): Boolean {
        val request = min(_capacity - amount, reloadRate)
        if (request <= 0) return false
        withOuterTransaction { tx ->
            val extracted = source.extract(INSTANCE, request, tx)
            if (extracted > 0) {
                _amount += extracted
                tx.commit()
                return true
            }
        }
        return false
    }

    override fun createSnapshot() = _amount
    override fun readSnapshot(snapshot: Long) {
        _amount = snapshot
    }

    override fun onFinalCommit() {
        listenable.listen()
    }

    override fun readFromNbt(nbt: NbtCompound) {
        _amount = nbt.getLong("amount")
        source.readFromNbt(nbt)
    }

    override fun toNbt(): NbtCompound =
        source.toNbt().apply {
            putLong("amount", _amount)
        }
}
