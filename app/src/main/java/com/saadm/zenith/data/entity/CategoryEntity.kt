package com.saadm.zenith.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class CategoryEntity(
	@PrimaryKey(autoGenerate = true) val id: Long = 0,
	val name: String,
	val emoji: String,
	val colorHex: String,
	val applicableTo: String,
	val sortOrder: Int,
	val isDefault: Boolean = false,
	val isDeleted: Boolean = false
)
