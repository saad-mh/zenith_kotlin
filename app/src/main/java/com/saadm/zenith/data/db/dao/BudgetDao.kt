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
}
