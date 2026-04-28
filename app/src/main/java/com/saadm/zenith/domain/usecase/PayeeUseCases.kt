package com.saadm.zenith.domain.usecase

import com.saadm.zenith.domain.model.Payee
import com.saadm.zenith.domain.repository.PayeeRepo
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Fetch all active payees with derived financial summaries.
 */
class GetPayeesUseCase @Inject constructor(
	private val payeeRepo: PayeeRepo
) {
	operator fun invoke(): Flow<List<Payee>> = payeeRepo.observeAllActive()
}

/**
 * Fetch a single payee with derived summary by ID.
 */
class GetPayeeSummaryUseCase @Inject constructor(
	private val payeeRepo: PayeeRepo
) {
	operator fun invoke(payeeId: Long): Flow<Payee?> = payeeRepo.observeById(payeeId)
}

