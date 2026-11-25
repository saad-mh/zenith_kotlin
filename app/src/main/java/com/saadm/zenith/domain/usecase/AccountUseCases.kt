package com.saadm.zenith.domain.usecase

import com.saadm.zenith.data.entity.AccountBalance
import com.saadm.zenith.data.entity.AccountEntity
import com.saadm.zenith.data.entity.TransactionEntity
import com.saadm.zenith.data.entity.TxnType
import com.saadm.zenith.domain.account.NetPosition
import com.saadm.zenith.domain.repository.AccountRepo
import com.saadm.zenith.domain.repository.CategoryRepo
import com.saadm.zenith.domain.repository.PayeeRepo
import com.saadm.zenith.domain.repository.TransactionRepo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

class ObserveAccountBalances @Inject constructor(private val accounts: AccountRepo) {
    operator fun invoke(): Flow<List<AccountBalance>> = accounts.observeBalances()
}

class SaveAccount @Inject constructor(private val accounts: AccountRepo) {
    suspend operator fun invoke(account: AccountEntity): Long {
        require(account.name.isNotBlank()) { "An account needs a name" }
        return accounts.save(account.copy(name = account.name.trim()))
    }
}

class ArchiveAccount @Inject constructor(private val accounts: AccountRepo) {
    suspend operator fun invoke(id: Long) = accounts.archive(id)
}

/**
 * Moves money between two accounts. Recorded as a TRANSFER_OUT on the source and a TRANSFER_IN on the
 * destination, written together. Transfers are not income or spending.
 */
class RecordTransfer @Inject constructor(
    private val transactions: TransactionRepo,
    private val categories: CategoryRepo
) {
    suspend operator fun invoke(
        fromAccountId: Long,
        toAccountId: Long,
        amount: Long,
        at: Long,
        note: String? = null,
        now: Long = System.currentTimeMillis()
    ): List<Long> {
        require(amount > 0) { "A transfer must be above zero" }
        require(fromAccountId != toAccountId) { "Choose two different accounts" }
        // Transfers need a category row. The default for each side is used.
        val outCategory = categories.defaultCategoryId(TxnType.EXPENSE) ?: error("No default category")
        val inCategory = categories.defaultCategoryId(TxnType.INCOME) ?: error("No default category")
        val cleanNote = note?.trim()?.ifEmpty { null }
        return transactions.saveAll(
            listOf(
                TransactionEntity(
                    amount = amount, type = TxnType.TRANSFER_OUT, transactedAt = at, createdAt = now, updatedAt = now,
                    categoryId = outCategory, payeeId = null, accountId = fromAccountId, note = cleanNote, receiptUri = null
                ),
                TransactionEntity(
                    amount = amount, type = TxnType.TRANSFER_IN, transactedAt = at, createdAt = now, updatedAt = now,
                    categoryId = inCategory, payeeId = null, accountId = toAccountId, note = cleanNote, receiptUri = null
                ),
            )
        )
    }
}

/** Accounts plus the friends' net, combined into one figure for Overview. */
class ObserveNetPosition @Inject constructor(
    private val accounts: AccountRepo,
    private val payees: PayeeRepo,
    private val transactions: TransactionRepo
) {
    operator fun invoke(): Flow<NetPosition> =
        combine(accounts.observeBalances(), ObserveSharedBalances(payees, transactions)()) { balances, shared ->
            NetPosition.of(balances, shared)
        }
}
