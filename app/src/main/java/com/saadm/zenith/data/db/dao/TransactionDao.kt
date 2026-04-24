package com.saadm.zenith.data.db.dao

import androidx.room.Dao
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

	@Query("SELECT * FROM transactions WHERE isDeleted = 0 ORDER BY transactedAt DESC, id DESC")
	fun observeAllActive(): Flow<List<TransactionEntity>>
}
