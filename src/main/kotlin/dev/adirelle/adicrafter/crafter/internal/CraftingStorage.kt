@file:Suppress("UnstableApiUsage")

package dev.adirelle.adicrafter.crafter.internal

import dev.adirelle.adicrafter.crafter.RecipeCrafter
import dev.adirelle.adicrafter.utils.Observable
import dev.adirelle.adicrafter.utils.Observer
import dev.adirelle.adicrafter.utils.extension.canExtract
import dev.adirelle.adicrafter.utils.extension.toAmount
import dev.adirelle.adicrafter.utils.general.ValueHolder
import dev.adirelle.adicrafter.utils.general.extensions.*
import dev.adirelle.adicrafter.utils.general.lazyLogger
import dev.adirelle.adicrafter.utils.minecraft.NbtPersistable
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant
import net.fabricmc.fabric.api.transfer.v1.storage.base.ResourceAmount
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext
import net.fabricmc.fabric.api.transfer.v1.transaction.base.SnapshotParticipant
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NbtCompound
import kotlin.math.max

class CraftingStorage(private val crafter: RecipeCrafter) :
    SnapshotParticipant<ResourceAmount<ItemVariant>>(),
    SingleSlotStorage<ItemVariant>,
    NbtPersistable<NbtCompound> {

    private val logger by lazyLogger

    private val contentHolder = ValueHolder<ResourceAmount<ItemVariant>>(EMPTY_ITEM_AMOUNT)
    private val contentObservable = Observable<ResourceAmount<ItemVariant>>()

    val content by contentHolder
    val recipe by crafter::recipe

    fun observeContent(observer: Observer<ResourceAmount<ItemVariant>>) =
        contentObservable.addObserver(observer)

    override fun createSnapshot() = content

    override fun readSnapshot(snapshot: ResourceAmount<ItemVariant>) {
        contentHolder.set(snapshot)
    }

    override fun onFinalCommit() {
        contentObservable.notify(content)
    }

    override fun supportsInsertion() = false
    override fun insert(resource: ItemVariant, maxAmount: Long, tx: TransactionContext) = 0L

    override fun extract(request: ItemVariant, maxAmount: Long, tx: TransactionContext): Long {
        if (!canExtract(request, resource)) return 0L
        updateSnapshots(tx)
        var available = content.amount
        if (available < maxAmount) {
            available += crafter.craft(maxAmount - available, tx)
        }
        if (available <= maxAmount) {
            contentHolder.set(EMPTY_ITEM_AMOUNT)
            return available
        }
        contentHolder.set(resource.toAmount(available - maxAmount))
        return maxAmount
    }

    override fun isResourceBlank() = recipe.isEmpty
    override fun getResource(): ItemVariant = recipe.output.resource
    override fun getAmount() = max(content.amount, recipe.output.amount)
    override fun getCapacity() = recipe.output.amount

    fun dropItems(): ItemStack {
        if (content.isEmpty()) return ItemStack.EMPTY
        val stack = content.toStack()
        contentHolder.set(EMPTY_ITEM_AMOUNT)
        return stack
    }

    fun readFromNbt(nbt: NbtCompound) {
        contentHolder.readFromNbt(nbt, ::resourceAmountFromNbt, EMPTY_ITEM_AMOUNT)
    }

    override fun toNbt(): NbtCompound =
        NbtCompound().apply {
            content.writeToNbt(this)
        }
}
