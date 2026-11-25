package com.saadm.zenith.domain.usecase

import com.saadm.zenith.data.entity.TransactionEntity
import com.saadm.zenith.data.entity.TxnType
import com.saadm.zenith.domain.payee.PayeeBalanceLine
import com.saadm.zenith.domain.payee.PayeeLedger
import com.saadm.zenith.domain.payee.SharedBalanceSummary
import com.saadm.zenith.domain.payee.SharedBalances
import com.saadm.zenith.domain.repository.CategoryRepo
import com.saadm.zenith.domain.repository.PayeeRepo
import com.saadm.zenith.domain.repository.TransactionRepo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Records a settlement: money moved to or from a payee. Partial, full or over-payment are all valid
 * amounts; the balance follows from the rows. Saved locally only, nothing is sent to the payee.
 */
class RecordSettlement @Inject constructor(
    private val transactions: TransactionRepo,
    private val categories: CategoryRepo
) {
    suspend operator fun invoke(
        payeeId: Long,
        theyPaidYou: Boolean,
        amount: Long,
        transactedAt: Long,
        accountId: Long? = null,
        note: String? = null,
        now: Long = System.currentTimeMillis()
    ): Long {
        require(amount > 0) { "A settlement must be above zero" }
        // A settlement needs a category row. The default for its direction is used, since settlements
        // are not income or spending.
        val categoryId = categories.defaultCategoryId(if (theyPaidYou) TxnType.INCOME else TxnType.EXPENSE)
            ?: error("No default category to file the settlement under")
        return transactions.save(
            TransactionEntity(
                amount = amount,
                type = if (theyPaidYou) TxnType.SETTLEMENT_FROM else TxnType.SETTLEMENT_TO,
                transactedAt = transactedAt,
                createdAt = now,
                updatedAt = now,
                categoryId = categoryId,
                payeeId = payeeId,
                accountId = accountId,
                note = note?.trim()?.ifEmpty { null },
                receiptUri = null
            )
        )
    }
}

class ObservePayeeLedger @Inject constructor(private val transactions: TransactionRepo) {
    operator fun invoke(payeeId: Long): Flow<PayeeLedger> =
        transactions.observeForPayee(payeeId).map { PayeeLedger.of(it) }
}

/** The shared-balance summary for Insights, from every payee's ledger. */
class ObserveSharedBalances @Inject constructor(
    private val payees: PayeeRepo,
    private val transactions: TransactionRepo
) {
    operator fun invoke(): Flow<SharedBalanceSummary> =
        combine(payees.observeAllActive(), transactions.observeAllActive()) { people, items ->
            val byPayee = items.filter { it.payeeId != null }.groupBy { it.payeeId!! }
            SharedBalances.summarize(
                people.map { person ->
                    val ledger = PayeeLedger.of(byPayee[person.id].orEmpty())
                    PayeeBalanceLine(person.id, person.name, ledger.net, ledger.oldestOpenAt)
                }
            )
        }
}
