package com.saadm.zenith.data.repository

import com.saadm.zenith.data.db.dao.BudgetDao
import com.saadm.zenith.data.entity.BudgetEntity
import com.saadm.zenith.domain.model.MonthPeriod
import com.saadm.zenith.domain.repository.BudgetRepo
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class BudgetRepoImpl @Inject constructor(
    private val dao: BudgetDao
) : BudgetRepo {

    override fun observeForPeriod(period: MonthPeriod): Flow<List<BudgetEntity>> =
        dao.observeForPeriod(period.startMillis)

    override suspend fun getForPeriod(period: MonthPeriod): List<BudgetEntity> =
        dao.getForPeriod(period.startMillis)

    override suspend fun save(budget: BudgetEntity): Long = dao.upsert(budget)

    override suspend fun deactivate(id: Long) = dao.deactivate(id)
}
