@file:Suppress("UnstableApiUsage")

package dev.adirelle.adicrafter.crafter.impl

import dev.adirelle.adicrafter.crafter.api.Crafter
import dev.adirelle.adicrafter.utils.extensions.toNbt
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant
import net.fabricmc.fabric.api.transfer.v1.storage.base.ExtractionOnlyStorage
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext
import net.fabricmc.fabric.api.transfer.v1.transaction.base.SnapshotParticipant
import net.minecraft.item.ItemConvertible
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NbtCompound
import net.minecraft.util.ItemScatterer
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import java.util.function.Supplier

class BufferedCrafter(
    private val crafterProvider: Supplier<Crafter>,
    private val onContentChanged: () -> Unit = {}
) : Crafter, ExtractionOnlyStorage<ItemVariant>, SnapshotParticipant<ItemStack>() {

    private val crafter: Crafter
        get() = crafterProvider.get()

    private var buffer: ItemStack = ItemStack.EMPTY

    override fun findIngredientFor(item: ItemConvertible) =
        crafter.findIngredientFor(item)

    fun dropBuffer(world: World, pos: BlockPos): Boolean {
        if (buffer.isEmpty) return false
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
        return true
    }

    fun dropBufferIfOutputMismatchs(world: World, pos: BlockPos): Boolean =
        !crafter.resource.matches(buffer) && dropBuffer(world, pos)

    fun readFromNbt(nbt: NbtCompound) {
        buffer = ItemStack.fromNbt(nbt)
    }

    fun toNbt(): NbtCompound =
        buffer.toNbt()

    override fun isResourceBlank() = crafter.isEmpty
    override fun getCapacity() = crafter.capacity
    override fun getResource() = crafter.resource
    override fun getAmount() = buffer.count.toLong()

    override fun extract(resource: ItemVariant, maxAmount: Long, tx: TransactionContext): Long {
        if (maxAmount < 1 || crafter.isEmpty || resource != crafter.resource) return 0L
        updateSnapshots(tx)
        val current = buffer.count.toLong()
        if (maxAmount > current) {
            val crafted = crafter.extract(resource, maxAmount - current, tx)
            if (buffer.isEmpty) {
                buffer = crafter.resource.toStack(crafted.toInt())
            } else {
                require(crafter.resource.matches(buffer)) { "crafter/buffer mismatch" }
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
        onContentChanged()
    }
}
