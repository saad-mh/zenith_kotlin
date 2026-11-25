package com.saadm.zenith.domain.repository

import com.saadm.zenith.data.entity.BudgetEntity
import com.saadm.zenith.domain.model.MonthPeriod
import kotlinx.coroutines.flow.Flow

/** Budgets are stored per month. Rules about which months may change live in the use cases. */
interface BudgetRepo {

    fun observeForPeriod(period: MonthPeriod): Flow<List<BudgetEntity>>

    suspend fun getForPeriod(period: MonthPeriod): List<BudgetEntity>

    /** Inserts a budget, or replaces the row with the same id. Returns the id. */
    suspend fun save(budget: BudgetEntity): Long

    /** Hides a budget. The row stays, so history is kept. */
    suspend fun deactivate(id: Long)
}
