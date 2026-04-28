package com.saadm.zenith.data.repository

import com.saadm.zenith.data.db.dao.PayeeBalanceDao
import com.saadm.zenith.data.db.dao.PayeeDao
import com.saadm.zenith.data.entity.PayeeEntity
import com.saadm.zenith.domain.model.Payee
import com.saadm.zenith.domain.repository.PayeeRepo
import kotlinx.coroutines.flow.Flow
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
	private val payeeBalanceDao: PayeeBalanceDao
) : PayeeRepo {

	override fun observeAllActive(): Flow<List<Payee>> {
		return payeeBalanceDao.observeAllActive().map { balances ->
			balances.map { balance ->
				Payee(
					id = balance.id,
					name = balance.name,
					avatarUri = balance.avatarUri,
					phone = balance.phone,
					upiId = balance.upiId,
					createdAt = balance.createdAt,
					dueToAmount = balance.dueToAmount,
					dueFromAmount = balance.dueFromAmount,
					netBalance = balance.netBalanceAmount(),
					transactionCount = balance.transactionCount,
					lastInteractionAt = balance.lastInteractionAt
				)
			}
		}
	}

	override fun observeById(id: Long): Flow<Payee?> {
		return payeeBalanceDao.observeById(id).map { balance ->
			balance?.let {
				Payee(
					id = it.id,
					name = it.name,
					avatarUri = it.avatarUri,
					phone = it.phone,
					upiId = it.upiId,
					createdAt = it.createdAt,
					dueToAmount = it.dueToAmount,
					dueFromAmount = it.dueFromAmount,
					netBalance = it.netBalanceAmount(),
					transactionCount = it.transactionCount,
					lastInteractionAt = it.lastInteractionAt
				)
			}
		}
	}

	override suspend fun getById(id: Long): Payee? {
		val balance = payeeBalanceDao.getById(id) ?: return null
		return Payee(
			id = balance.id,
			name = balance.name,
			avatarUri = balance.avatarUri,
			phone = balance.phone,
			upiId = balance.upiId,
			createdAt = balance.createdAt,
			dueToAmount = balance.dueToAmount,
			dueFromAmount = balance.dueFromAmount,
			netBalance = balance.netBalanceAmount(),
			transactionCount = balance.transactionCount,
			lastInteractionAt = balance.lastInteractionAt
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

