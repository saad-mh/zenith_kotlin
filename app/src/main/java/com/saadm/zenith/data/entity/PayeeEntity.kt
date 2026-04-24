package com.saadm.zenith.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "payees")
data class PayeeEntity(
	@PrimaryKey(autoGenerate = true) val id: Long = 0,
	val name: String,
	val avatarUri: String?,
	val phone: String?,
	val upiId: String?,
	val createdAt: Long,
	val isDeleted: Boolean = false
)
