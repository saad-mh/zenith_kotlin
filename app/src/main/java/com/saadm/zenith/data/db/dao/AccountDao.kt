package com.saadm.zenith.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.saadm.zenith.data.entity.AccountEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {
	@Insert(onConflict = OnConflictStrategy.REPLACE)
	suspend fun upsert(account: AccountEntity): Long

	@Update
	suspend fun update(account: AccountEntity)

	@Query("SELECT * FROM accounts WHERE isDeleted = 0 ORDER BY sortOrder ASC, id ASC")
	fun observeAllActive(): Flow<List<AccountEntity>>
}
