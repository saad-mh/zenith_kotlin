package com.saadm.zenith.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.saadm.zenith.data.entity.AccountBalance
import com.saadm.zenith.data.entity.AccountEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {
	@Insert(onConflict = OnConflictStrategy.ABORT)
	suspend fun insert(account: AccountEntity): Long

	// Update rather than REPLACE: REPLACE deletes the old row first, which would null out
	// the account on every transaction that uses it.
	@Update
	suspend fun update(account: AccountEntity)

	@Query("SELECT * FROM accounts WHERE isDeleted = 0 ORDER BY sortOrder ASC, id ASC")
	fun observeAllActive(): Flow<List<AccountEntity>>

	// Money in: income and settlements they paid, and transfers in. Money out: expenses,
	// settlements you paid, and transfers out. Dues move no money, so they are not counted.
	@Query(
		"""
		SELECT a.id, a.name, a.type, a.colorHex, a.emoji, a.sortOrder, a.isDeleted, a.openingBalance,
			a.openingBalance + COALESCE(SUM(CASE WHEN t.isDeleted = 0 THEN
				CASE t.type
					WHEN 'INCOME' THEN t.amount
					WHEN 'SETTLEMENT_FROM' THEN t.amount
					WHEN 'TRANSFER_IN' THEN t.amount
					WHEN 'EXPENSE' THEN -t.amount
					WHEN 'SETTLEMENT_TO' THEN -t.amount
					WHEN 'TRANSFER_OUT' THEN -t.amount
					ELSE 0
				END
			ELSE 0 END), 0) AS balance
		FROM accounts a
		LEFT JOIN transactions t ON t.accountId = a.id
		WHERE a.isDeleted = 0
		GROUP BY a.id
		ORDER BY a.sortOrder ASC, a.id ASC
		"""
	)
	fun observeBalances(): Flow<List<AccountBalance>>

	@Query("UPDATE accounts SET isDeleted = 1 WHERE id = :id")
	suspend fun archive(id: Long)
}
