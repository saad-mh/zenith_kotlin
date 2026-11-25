package com.saadm.zenith.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "accounts")
data class AccountEntity(
	@PrimaryKey(autoGenerate = true) val id: Long = 0,
	val name: String,
	val type: AccountType,
	val colorHex: String,
	val emoji: String,
	val sortOrder: Int,
	val isDeleted: Boolean = false,
	/** Balance before the first recorded transaction, in paise. Added in version 8. */
	@ColumnInfo(defaultValue = "0") val openingBalance: Long = 0L
)
