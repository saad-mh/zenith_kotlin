package com.saadm.zenith.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.saadm.zenith.data.entity.BudgetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetDao {
	@Insert(onConflict = OnConflictStrategy.REPLACE)
	suspend fun upsert(budget: BudgetEntity): Long

	@Update
	suspend fun update(budget: BudgetEntity)

	@Query("SELECT * FROM budgets WHERE isActive = 1")
	fun observeActiveBudgets(): Flow<List<BudgetEntity>>

	// A budget belongs to one month: its startDate is the first millisecond of that month.
	@Query(
		"""
		SELECT * FROM budgets
		WHERE isActive = 1 AND startDate = :periodStartMillis
		ORDER BY (categoryId IS NULL) DESC, id ASC
		"""
	)
	fun observeForPeriod(periodStartMillis: Long): Flow<List<BudgetEntity>>

	@Query(
		"""
		SELECT * FROM budgets
		WHERE isActive = 1 AND startDate = :periodStartMillis
		ORDER BY (categoryId IS NULL) DESC, id ASC
		"""
	)
	suspend fun getForPeriod(periodStartMillis: Long): List<BudgetEntity>

	@Query("UPDATE budgets SET isActive = 0 WHERE id = :id")
	suspend fun deactivate(id: Long)
}
