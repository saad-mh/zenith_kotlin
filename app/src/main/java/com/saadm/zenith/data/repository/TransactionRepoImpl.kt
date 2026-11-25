package com.saadm.zenith.data.repository

import com.saadm.zenith.data.db.dao.TransactionDao
import com.saadm.zenith.data.entity.TransactionEntity
import com.saadm.zenith.domain.repository.TransactionRepo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject

class TransactionRepoImpl @Inject constructor(
    private val dao: TransactionDao
) : TransactionRepo {

    override fun observeAllActive(): Flow<List<TransactionEntity>> = dao.observeAllActive()

    override fun getRecentTransactions(): Flow<List<TransactionEntity>> = dao.getRecentTransactions()

    override suspend fun delete(transaction: TransactionEntity) = dao.delete(transaction)

    override suspend fun save(transaction: TransactionEntity): Long = dao.upsert(transaction)

    override fun observeForPayee(payeeId: Long): Flow<List<TransactionEntity>> = dao.observeActiveByPayeeId(payeeId)

    override fun observeBetween(startMillis: Long, endMillis: Long): Flow<List<TransactionEntity>> =
        dao.observeBetween(startMillis, endMillis)

    override fun observeSearch(query: String): Flow<List<TransactionEntity>> {
        val term = query.trim()
        if (term.isEmpty()) return flowOf(emptyList())
        return dao.observeSearch("%${escapeLike(term)}%")
    }

    // Escape LIKE wildcards so a search for "50%" or "a_b" matches literally.
    private fun escapeLike(term: String): String =
        term.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")
}
