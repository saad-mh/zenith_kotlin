package com.saadm.zenith.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.saadm.zenith.data.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
	@Insert(onConflict = OnConflictStrategy.REPLACE)
	suspend fun upsert(transaction: TransactionEntity): Long

	@Update
	suspend fun update(transaction: TransactionEntity)

	@Insert(onConflict = OnConflictStrategy.REPLACE)
	suspend fun upsertAll(transactions: List<TransactionEntity>): List<Long>

	@Delete
	suspend fun delete(transaction: TransactionEntity)

	@Delete
	suspend fun deleteAll(transactions: List<TransactionEntity>)

	@Query("SELECT * FROM transactions WHERE isDeleted = 0 ORDER BY transactedAt DESC, id DESC")
	fun observeAllActive(): Flow<List<TransactionEntity>>

	// Start is inclusive, end is exclusive, so a transaction on a boundary falls in exactly one period.
	@Query(
		"""
		SELECT * FROM transactions
		WHERE isDeleted = 0
		  AND transactedAt >= :startMillis
		  AND transactedAt < :endMillis
		ORDER BY transactedAt DESC, id DESC
		"""
	)
	fun observeBetween(startMillis: Long, endMillis: Long): Flow<List<TransactionEntity>>

	// :pattern is a LIKE pattern, already escaped by the caller.
	@Query(
		"""
		SELECT t.* FROM transactions t
		LEFT JOIN categories c ON c.id = t.categoryId
		LEFT JOIN payees p ON p.id = t.payeeId
		WHERE t.isDeleted = 0
		  AND (t.note LIKE :pattern ESCAPE '\' OR c.name LIKE :pattern ESCAPE '\' OR p.name LIKE :pattern ESCAPE '\')
		ORDER BY t.transactedAt DESC, t.id DESC
		"""
	)
	fun observeSearch(pattern: String): Flow<List<TransactionEntity>>

	@Query(
		"""
		SELECT * FROM transactions
		WHERE payeeId = :payeeId
		  AND isDeleted = 0
		  AND type IN ('DUE_TO', 'DUE_FROM', 'SETTLEMENT_FROM', 'SETTLEMENT_TO')
		ORDER BY transactedAt DESC, id DESC
		"""
	)
	fun observeActiveByPayeeId(payeeId: Long): Flow<List<TransactionEntity>>

	@Query(
		"""
		SELECT * FROM transactions
		WHERE payeeId = :payeeId
		  AND isDeleted = 0
		  AND type IN ('DUE_TO', 'DUE_FROM', 'SETTLEMENT_FROM', 'SETTLEMENT_TO')
		ORDER BY transactedAt DESC, id DESC
		"""
	)
	suspend fun getActiveByPayeeId(payeeId: Long): List<TransactionEntity>


	// Pages keep an export from loading the whole history at once.
	@Query("SELECT * FROM transactions ORDER BY id ASC LIMIT :limit OFFSET :offset")
	suspend fun page(limit: Int, offset: Int): List<TransactionEntity>
}
