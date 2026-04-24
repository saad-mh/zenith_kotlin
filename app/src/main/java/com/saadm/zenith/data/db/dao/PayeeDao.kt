package com.saadm.zenith.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.saadm.zenith.data.entity.PayeeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PayeeDao {
	@Insert(onConflict = OnConflictStrategy.REPLACE)
	suspend fun upsert(payee: PayeeEntity): Long

	@Update
	suspend fun update(payee: PayeeEntity)

	@Query("SELECT * FROM payees WHERE isDeleted = 0 ORDER BY name COLLATE NOCASE ASC")
	fun observeAllActive(): Flow<List<PayeeEntity>>
}
