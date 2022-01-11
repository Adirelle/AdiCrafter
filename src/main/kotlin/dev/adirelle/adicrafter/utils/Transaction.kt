@file:Suppress("UnstableApiUsage")

package dev.adirelle.adicrafter.utils

import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext

/**
 * Execute the block in an outer transaction.
 *
 * Finally, close the transaction at the end of the block.
 */
inline fun <T> withOuterTransaction(block: (Transaction) -> T): T =
    Transaction.openOuter().use(block)

/**
 * Execute the block in a nested transaction if possible, else create and use an outer transaction.
 *
 * Finally, close the transaction at the end of the block.
 */
inline fun <T> withNestedTransaction(txc: TransactionContext?, block: (Transaction) -> T): T =
    Transaction.openNested(txc).use(block)
