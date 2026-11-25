package com.saadm.zenith.domain.repository

import com.saadm.zenith.data.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow

/** Reads transactions. Summaries are computed from these lists by the analytics, not stored. */
interface TransactionRepo {

    fun observeAllActive(): Flow<List<TransactionEntity>>

    fun getRecentTransactions(): Flow<List<TransactionEntity>>

    /** Hard delete: the row is removed, not flagged. */
    suspend fun delete(transaction: TransactionEntity)

    /** Inserts or replaces a transaction. Returns its id. */
    suspend fun save(transaction: TransactionEntity): Long

    /** Payee dues and settlements for one payee, newest first. */
    fun observeForPayee(payeeId: Long): Flow<List<TransactionEntity>>

    /** Transactions with `transactedAt` in [startMillis, endMillis), newest first. */
    fun observeBetween(startMillis: Long, endMillis: Long): Flow<List<TransactionEntity>>

    /** Matches the note, category name or payee name, ignoring case. A blank query matches nothing. */
    fun observeSearch(query: String): Flow<List<TransactionEntity>>
}
