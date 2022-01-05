@file:Suppress("UnstableApiUsage")

package dev.adirelle.adicrafter.utils.extension

import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext

inline fun <T> inOuterTransaction(crossinline block: (Transaction) -> T): T =
    inTransaction(Transaction.openOuter(), block)

inline fun <T> inNestedTransaction(txc: TransactionContext?, crossinline block: (Transaction) -> T): T =
    inTransaction(Transaction.openNested(txc), block)

inline fun <T> inTransaction(txc: TransactionContext?, crossinline block: (Transaction) -> T): T =
    ((txc as? Transaction) ?: Transaction.openOuter()).use { block(it) }
