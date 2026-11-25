package com.saadm.zenith.data.entity

/** An active account with its balance: the opening balance plus every cash movement on it. */
data class AccountBalance(
	val id: Long,
	val name: String,
	val type: AccountType,
	val colorHex: String,
	val emoji: String,
	val sortOrder: Int,
	val isDeleted: Boolean,
	val openingBalance: Long,
	val balance: Long
)
