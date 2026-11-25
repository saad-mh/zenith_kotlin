package com.saadm.zenith.data.db.dao

import androidx.room.Dao
import androidx.room.Upsert
import androidx.room.OnConflictStrategy
import androidx.room.Query
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


	@Query("SELECT * FROM categories ORDER BY id ASC")
	suspend fun getAll(): List<CategoryEntity>
}
