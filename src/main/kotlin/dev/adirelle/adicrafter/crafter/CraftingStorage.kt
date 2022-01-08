@file:Suppress("UnstableApiUsage")

package dev.adirelle.adicrafter.crafter

import dev.adirelle.adicrafter.utils.extension.toItemString
import dev.adirelle.adicrafter.utils.extension.withNestedTransaction
import dev.adirelle.adicrafter.utils.general.lazyLogger
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext

class CraftingStorage(
    private val buffer: SingleSlotStorage<ItemVariant>,
    private val crafter: SingleSlotStorage<ItemVariant>
) :
    SingleSlotStorage<ItemVariant> {

    private val logger by lazyLogger

    override fun extract(resource: ItemVariant, maxAmount: Long, txc: TransactionContext?) =
        withNestedTransaction(txc) { tx ->
            val bufferedResource = buffer.resource
            var actualResource = resource
            var extracted = buffer.extract(actualResource, maxAmount, tx)
            logger.info("extracted {} {}s from buffer", extracted, toItemString(bufferedResource))
            if (extracted > 0) {
                actualResource = bufferedResource
            }
            if (extracted < maxAmount) {
                val craftedResource = crafter.resource
                val crafted = crafter.extract(actualResource, maxAmount - extracted, tx)
                logger.info("extracted {} {}s from buffer", crafted, toItemString(craftedResource))
                if (crafted > 0) {
                    actualResource = craftedResource
                    extracted += crafted
                }
            }
            val overflow = extracted - maxAmount
            logger.info(
                "requested: {} {}s, extracted: {} {}s, overflow: {}",
                maxAmount,
                toItemString(resource),
                extracted,
                toItemString(actualResource),
                overflow
            )
            if (overflow > 0) {
                val putBack = buffer.insert(actualResource, overflow, tx)
                logger.info("put back {} {}s into the buffer", putBack, toItemString(actualResource))
                if (putBack == overflow) {
                    maxAmount
                } else {
                    logger.info("could not putback all items, aborting")
                    tx.abort()
                    0L
                }
            } else
                extracted
        }

    private fun resolvedView(): StorageView<ItemVariant> =
        if (buffer.isResourceBlank) crafter
        else buffer

    override fun isResourceBlank() = resolvedView().isResourceBlank
    override fun getResource(): ItemVariant = resolvedView().resource
    override fun getAmount() = resolvedView().amount
    override fun getCapacity() = resolvedView().capacity

    override fun supportsInsertion() = false
    override fun insert(resource: ItemVariant?, maxAmount: Long, transaction: TransactionContext?) = 0L
}
