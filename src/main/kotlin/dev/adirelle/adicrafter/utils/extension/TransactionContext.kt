@file:Suppress("UnstableApiUsage")

package dev.adirelle.adicrafter.utils.extension

import dev.adirelle.adicrafter.utils.lazyLogger
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext

//inline fun <T> TransactionContext?.use(block: (Transaction) -> T): T =
//    transactional(this, block)

val logger by lazyLogger("transactional")

inline fun <T> transactional(crossinline block: (Transaction) -> T): T =
    transactional(null, block)

inline fun <T> transactional(txc: TransactionContext?, crossinline block: (Transaction) -> T): T =
    Transaction.openNested(txc).use { tx ->
        logger.info("opening tx #{}", tx.nestingDepth())
        tx.addCloseCallback { t, r -> logger.info("closing tx #{}: {}", t.nestingDepth(), r) }
        val result = block(tx)
        tx.commit()
        result
    }
