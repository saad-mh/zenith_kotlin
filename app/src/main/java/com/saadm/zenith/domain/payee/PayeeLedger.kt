package com.saadm.zenith.domain.payee

import com.saadm.zenith.data.entity.TransactionEntity
import com.saadm.zenith.data.entity.TxnType

enum class BalanceDirection { OWES_YOU, YOU_OWE, SETTLED }

/** One dues or settlement item, and the balance after it. Positive balance means the payee owes the user. */
data class LedgerEntry(
    val transactionId: Long,
    val type: TxnType,
    val amount: Long,
    val at: Long,
    val runningBalance: Long
)

/**
 * A payee's rolling balance, with its history. Dues and settlements are signed from the user's side:
 * a due to the user (DUE_TO) and a settlement you paid (SETTLEMENT_TO) move the balance up,
 * a due from the user (DUE_FROM) and a settlement they paid (SETTLEMENT_FROM) move it down.
 *
 * [oldestOpenAt] is the first item since the balance last sat at zero, so a debt that was
 * settled and then reopened counts from the reopening. [openDueCount] counts the dues in that run.
 */
data class PayeeLedger(
    val net: Long,
    val direction: BalanceDirection,
    val oldestOpenAt: Long?,
    val openDueCount: Int,
    val history: List<LedgerEntry>
) {
    companion object {
        private val PAYEE_TYPES = setOf(
            TxnType.DUE_TO, TxnType.DUE_FROM, TxnType.SETTLEMENT_FROM, TxnType.SETTLEMENT_TO
        )

        fun of(transactions: List<TransactionEntity>): PayeeLedger {
            val items = transactions
                .filter { !it.isDeleted && it.type in PAYEE_TYPES }
                .sortedWith(compareBy({ it.transactedAt }, { it.id }))

            var running = 0L
            var runStart = 0
            val history = items.mapIndexed { index, txn ->
                running += signed(txn)
                if (running == 0L) runStart = index + 1
                LedgerEntry(txn.id, txn.type, txn.amount, txn.transactedAt, running)
            }

            val open = items.drop(runStart)
            val direction = when {
                running > 0 -> BalanceDirection.OWES_YOU
                running < 0 -> BalanceDirection.YOU_OWE
                else -> BalanceDirection.SETTLED
            }
            return PayeeLedger(
                net = running,
                direction = direction,
                oldestOpenAt = if (direction == BalanceDirection.SETTLED) null else open.first().transactedAt,
                openDueCount = open.count { it.type == TxnType.DUE_TO || it.type == TxnType.DUE_FROM },
                history = history
            )
        }

        private fun signed(txn: TransactionEntity): Long = when (txn.type) {
            TxnType.DUE_TO, TxnType.SETTLEMENT_TO -> txn.amount
            TxnType.DUE_FROM, TxnType.SETTLEMENT_FROM -> -txn.amount
            else -> 0L
        }
    }
}
