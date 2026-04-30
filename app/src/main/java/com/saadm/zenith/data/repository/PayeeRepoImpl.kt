package com.saadm.zenith.data.repository

import com.saadm.zenith.data.db.dao.PayeeBalanceDao
import com.saadm.zenith.data.db.dao.PayeeDao
import com.saadm.zenith.data.db.dao.TransactionDao
import com.saadm.zenith.data.entity.PayeeBalanceView
import com.saadm.zenith.data.entity.PayeeEntity
import com.saadm.zenith.data.entity.TransactionEntity
import com.saadm.zenith.domain.model.Payee
import com.saadm.zenith.domain.repository.PayeeRepo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Data layer implementation of PayeeRepo.
 *
 * Combines PayeeEntity (stored data) with PayeeBalanceView (derived summaries)
 * to provide a complete Payee domain model without storing redundant aggregates.
 */
class PayeeRepoImpl @Inject constructor(
	private val payeeDao: PayeeDao,
	private val payeeBalanceDao: PayeeBalanceDao,
	private val transactionDao: TransactionDao
) : PayeeRepo {

	override fun observeAllActive(): Flow<List<Payee>> {
		return payeeBalanceDao.observeAllActive().map { balances ->
			balances.map { balance ->
				balance.toDomain()
			}
		}
	}

	override fun observeById(id: Long): Flow<Payee?> {
		return combine(
			payeeBalanceDao.observeById(id),
			transactionDao.observeActiveByPayeeId(id)
		) { balance, transactions ->
			balance?.toDomain(transactions)
		}
	}

	override suspend fun getById(id: Long): Payee? {
		val balance = payeeBalanceDao.getById(id) ?: return null
		val transactions = transactionDao.getActiveByPayeeId(id)
		return balance.toDomain(transactions)
	}

	private fun PayeeBalanceView.toDomain(transactions: List<TransactionEntity> = emptyList()): Payee {
		return Payee(
			id = id,
			name = name,
			avatarUri = avatarUri,
			phone = phone,
			upiId = upiId,
			createdAt = createdAt,
			dueToAmount = dueToAmount,
			dueFromAmount = dueFromAmount,
			netBalance = netBalanceAmount(),
			transactionCount = transactionCount,
			lastInteractionAt = lastInteractionAt,
			transactions = transactions
		)
	}

	override suspend fun upsert(payee: PayeeEntity): Long {
		return payeeDao.upsert(payee)
	}

	override suspend fun update(payee: PayeeEntity) {
		return payeeDao.update(payee)
	}

	override suspend fun softDelete(id: Long) {
		return payeeDao.softDelete(id)
	}
}

