package com.saadm.zenith.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import com.saadm.zenith.data.entity.PayeeBalanceView
import kotlinx.coroutines.flow.Flow

/**
 * DAO for querying payee balance summaries.
 * Payee financial data is derived from the underlying transactions,
 * ensuring consistency at the database level.
 */
@Dao
interface PayeeBalanceDao {
	@Query("SELECT * FROM payee_balances WHERE isDeleted = 0 ORDER BY name COLLATE NOCASE ASC")
	fun observeAllActive(): Flow<List<PayeeBalanceView>>

	@Query("SELECT * FROM payee_balances WHERE id = :id LIMIT 1")
	fun observeById(id: Long): Flow<PayeeBalanceView?>

	@Query("SELECT * FROM payee_balances WHERE id = :id LIMIT 1")
	suspend fun getById(id: Long): PayeeBalanceView?
}

