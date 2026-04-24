package com.saadm.zenith.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.saadm.zenith.data.entity.CategoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
	@Insert(onConflict = OnConflictStrategy.REPLACE)
	suspend fun upsert(category: CategoryEntity): Long

	@Update
	suspend fun update(category: CategoryEntity)

	@Query("SELECT * FROM categories WHERE isDeleted = 0 ORDER BY sortOrder ASC, id ASC")
	fun observeAllActive(): Flow<List<CategoryEntity>>
}
