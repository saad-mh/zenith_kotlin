package com.saadm.zenith.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "budgets")
data class BudgetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val categoryId: Long?,
    val limitAmount: Long,
    val periodType: BudgetPeriod,
    val startDate: Long?,
    val endDate: Long?,
    val currency: String = "INR",
    val isActive: Boolean = true
)
