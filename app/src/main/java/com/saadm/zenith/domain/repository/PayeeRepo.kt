package com.saadm.zenith.domain.repository

import com.saadm.zenith.data.entity.PayeeEntity
import com.saadm.zenith.domain.model.Payee
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for payee operations.
 *
 * All financial summaries (balances, interaction counts) are derived dynamically from
 * the transactions dataset, ensuring the system maintains a single source of truth.
 */
interface PayeeRepo {
	/**
	 * Observe all non-deleted payees, ordered alphabetically.
	 * Includes derived financial summaries from related transactions.
	 */
	fun observeAllActive(): Flow<List<Payee>>

	/**
	 * Observe a single payee by ID with derived summaries.
	 */
	fun observeById(id: Long): Flow<Payee?>

	/**
	 * Get a payee by ID synchronously (for one-off fetches).
	 */
	suspend fun getById(id: Long): Payee?

	/**
	 * Insert or update a payee.
	 * Returns the ID (new or existing).
	 */
	suspend fun upsert(payee: PayeeEntity): Long

	/**
	 * Update a payee.
	 */
	suspend fun update(payee: PayeeEntity)

	/**
	 * Soft-delete a payee by setting isDeleted = 1.
	 */
	suspend fun softDelete(id: Long)
}
