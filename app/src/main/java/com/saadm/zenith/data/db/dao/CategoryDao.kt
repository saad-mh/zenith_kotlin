package com.saadm.zenith.data.db.dao

import androidx.room.Dao
import androidx.room.Upsert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.saadm.zenith.data.entity.CategoryEntity
import com.saadm.zenith.data.entity.TxnType
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
	@Upsert
	suspend fun upsert(category: CategoryEntity): Long

	@Update
	suspend fun update(category: CategoryEntity)

	@Query("SELECT * FROM categories WHERE isDeleted = 0 ORDER BY sortOrder ASC, id ASC")
	fun observeAllActive(): Flow<List<CategoryEntity>>

	@Query("SELECT * FROM categories WHERE isDeleted = 0 AND txnType = :txnType ORDER BY sortOrder ASC, id ASC")
	fun observeActiveByType(txnType: String): Flow<List<CategoryEntity>>

	@Query("SELECT * FROM categories WHERE isDeleted = 0 AND txnType = :txnType AND isDefault = 1 LIMIT 1")
	suspend fun getDefaultByType(txnType: String): CategoryEntity?

	@Query("SELECT * FROM categories WHERE isDeleted = 0 AND txnType = :txnType ORDER BY sortOrder ASC, id ASC LIMIT 1")
	suspend fun getFirstByType(txnType: String): CategoryEntity?


	@Query("SELECT * FROM categories ORDER BY sortOrder ASC, id ASC")
	fun observeEvery(): Flow<List<CategoryEntity>>

	@Query("UPDATE categories SET isDeleted = 1 WHERE id = :id")
	suspend fun archive(id: Long)

	@Query("UPDATE transactions SET categoryId = :into WHERE categoryId = :from")
	suspend fun moveTransactions(from: Long, into: Long)

	// A month keeps one budget per category, so the source's gives way where the target has one.
	@Query(
		"""
		UPDATE budgets SET isActive = 0 WHERE categoryId = :from AND startDate IN
			(SELECT startDate FROM budgets WHERE categoryId = :into AND isActive = 1)
		"""
	)
	suspend fun dropClashingBudgets(from: Long, into: Long)

	@Query("UPDATE budgets SET categoryId = :into WHERE categoryId = :from")
	suspend fun moveBudgets(from: Long, into: Long)

	@Transaction
	suspend fun merge(from: Long, into: Long) {
		moveTransactions(from, into)
		dropClashingBudgets(from, into)
		moveBudgets(from, into)
		archive(from)
	}

	@Query("SELECT * FROM categories ORDER BY id ASC")
	suspend fun getAll(): List<CategoryEntity>
}
