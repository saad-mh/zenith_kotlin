package com.saadm.zenith.domain.repository

import com.saadm.zenith.data.db.dao.TransactionDao
import com.saadm.zenith.data.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class TransactionRepo @Inject constructor(private val dao: TransactionDao) {

    fun observeAllActive(): Flow<List<TransactionEntity>> {
        return dao.observeAllActive()
    }

    fun getRecentTransactions() : Flow<List<TransactionEntity>> {
        return dao.getRecentTransactions()
    }

    fun delete(transaction: TransactionEntity) {
        TODO("stop being a dumbass and complete this")
    }
}
