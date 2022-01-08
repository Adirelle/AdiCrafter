@file:Suppress("UnstableApiUsage")

package dev.adirelle.adicrafter.utils.extension

import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext

/**
 * Execute the block in an outer transaction.
 *
 * Finally, close the transaction at the end of the block.
 */
inline fun <T> withOuterTransaction(crossinline block: (Transaction) -> T): T =
    Transaction.openOuter().use(block)

/**
 * Execute the block in a nested transaction if possible, else create and use an outer transaction.
 *
 * Finally, close the transaction at the end of the block.
 */
inline fun <T> withNestedTransaction(txc: TransactionContext?, crossinline block: (Transaction) -> T): T =
    Transaction.openNested(txc).use(block)

/**
 * Execute the block in a transaction.
 *
 * If the passed argument is an existing transaction, use it. Else create an outer transaction
 * that will be finally closed at the end of the block.
 */
inline fun <T : Any> withTransaction(txc: TransactionContext?, crossinline block: (Transaction) -> T): T =
    (txc as? Transaction)?.let(block) ?: Transaction.openOuter().use(block)
