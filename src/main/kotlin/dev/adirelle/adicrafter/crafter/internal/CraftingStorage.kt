@file:Suppress("UnstableApiUsage")

package dev.adirelle.adicrafter.crafter.internal

import dev.adirelle.adicrafter.crafter.RecipeCrafter
import dev.adirelle.adicrafter.utils.Observable
import dev.adirelle.adicrafter.utils.Observer
import dev.adirelle.adicrafter.utils.extension.canExtract
import dev.adirelle.adicrafter.utils.extension.toAmount
import dev.adirelle.adicrafter.utils.general.ValueHolder
import dev.adirelle.adicrafter.utils.general.extensions.*
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
    SnapshotParticipant<CraftingStorage.Snapshot>(),
    SingleSlotStorage<ItemVariant>,
    NbtPersistable<NbtCompound> {

    private var contentChanged = false
    private val contentHolder: ValueHolder<ResourceAmount<ItemVariant>> =
        object : ValueHolder<ResourceAmount<ItemVariant>>(EMPTY_ITEM_AMOUNT) {
            override fun onValueChanged(oldValue: ResourceAmount<ItemVariant>, newValue: ResourceAmount<ItemVariant>) {
                contentChanged = true
            }
        }
    private val contentObservable = Observable<ResourceAmount<ItemVariant>>()

    val content by contentHolder
    val recipe by crafter::recipe

    fun observeContent(observer: Observer<ResourceAmount<ItemVariant>>) =
        contentObservable.addObserver(observer)

    override fun createSnapshot() = Snapshot(content, contentChanged)

    override fun readSnapshot(snapshot: Snapshot) {
        contentHolder.set(snapshot.content)
        contentChanged = snapshot.changed
    }

    override fun onFinalCommit() {
        if (contentChanged) {
            contentChanged = false
            contentObservable.notify(content)
        }
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
        contentHolder.set(content.withAmount(available - maxAmount))
        return maxAmount
    }

    override fun isResourceBlank() = recipe.isEmpty
    override fun getResource(): ItemVariant = recipe.output.resource
    override fun getAmount() = max(content.amount, recipe.output.amount)
    override fun getCapacity() = recipe.output.amount

    fun dropItems(): ItemStack {
        if (content.isEmpty() || content.resource == recipe.output.resource) return ItemStack.EMPTY
        val stack = content.toStack()
        contentHolder.set(EMPTY_ITEM_AMOUNT)
        return stack
    }

    fun computeForecast(): ResourceAmount<ItemVariant> {
        val maxAmount = recipe.output.amount
        var forecastAmount = content.amount
        if (forecastAmount < maxAmount) {
            forecastAmount += crafter.simulateCraft(maxAmount - forecastAmount)
        }
        return recipe.output.resource.toAmount(forecastAmount)
    }

    fun readFromNbt(nbt: NbtCompound) {
        contentHolder.readFromNbt(nbt, ::resourceAmountFromNbt, EMPTY_ITEM_AMOUNT)
    }

    override fun toNbt(): NbtCompound =
        NbtCompound().apply {
            content.writeToNbt(this)
        }

    data class Snapshot(val content: ResourceAmount<ItemVariant>, val changed: Boolean)
}
