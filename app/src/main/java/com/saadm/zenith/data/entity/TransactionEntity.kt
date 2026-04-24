package com.saadm.zenith.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
	tableName = "transactions",
	foreignKeys = [
		ForeignKey(
			entity = PayeeEntity::class,
			parentColumns = ["id"],
			childColumns = ["payeeId"],
			onDelete = ForeignKey.SET_NULL
		),
		ForeignKey(
			entity = AccountEntity::class,
			parentColumns = ["id"],
			childColumns = ["accountId"],
			onDelete = ForeignKey.SET_NULL
		),
		ForeignKey(
			entity = CategoryEntity::class,
			parentColumns = ["id"],
			childColumns = ["categoryId"],
			onDelete = ForeignKey.RESTRICT
		)
	],
	indices = [
		Index("payeeId"),
		Index("accountId"),
		Index("categoryId"),
		Index("transactedAt"),
		Index("isDeleted")
	]
)
data class TransactionEntity(
	@PrimaryKey(autoGenerate = true) val id: Long = 0,
	val amount: Long,
	val type: TxnType,
	val transactedAt: Long,
	val createdAt: Long,
	val updatedAt: Long,
	val categoryId: Long,
	val payeeId: Long?,
	val accountId: Long?,
	val note: String?,
	val receiptUri: String?,
	val currency: String = "INR",
	val isDeleted: Boolean = false,
	val deletedAt: Long? = null
)
