package com.saadm.zenith.domain.usecase

import com.saadm.zenith.data.entity.BudgetEntity
import com.saadm.zenith.data.entity.BudgetPeriod
import com.saadm.zenith.domain.analytics.SpendAnalytics
import com.saadm.zenith.domain.budget.BudgetRules
import com.saadm.zenith.domain.budget.MonthBudgets
import com.saadm.zenith.domain.model.MonthPeriod
import com.saadm.zenith.domain.model.MonthPeriods
import com.saadm.zenith.domain.repository.BudgetRepo
import com.saadm.zenith.domain.repository.TransactionRepo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

class ObserveMonthBudgets @Inject constructor(
    private val budgets: BudgetRepo,
    private val transactions: TransactionRepo
) {
    operator fun invoke(period: MonthPeriod, today: LocalDate): Flow<MonthBudgets> =
        combine(
            budgets.observeForPeriod(period),
            transactions.observeBetween(period.startMillis, period.endMillis)
        ) { rows, txns -> BudgetRules.summarize(period, rows, txns, today) }
}

/** Creates or changes a budget. Past months are read-only. A null category is the overall budget. */
class SaveBudget @Inject constructor(private val budgets: BudgetRepo) {
    suspend operator fun invoke(
        period: MonthPeriod,
        today: LocalDate,
        categoryId: Long?,
        limit: Long
    ): Long {
        require(BudgetRules.isEditable(period, today)) { "Past months are read-only" }
        require(limit > 0) { "A budget must be above zero" }
        val existing = budgets.getForPeriod(period).firstOrNull { it.categoryId == categoryId }
        return budgets.save(
            BudgetEntity(
                id = existing?.id ?: 0,
                categoryId = categoryId,
                limitAmount = limit,
                periodType = BudgetPeriod.MONTHLY,
                startDate = period.startMillis,
                endDate = period.endMillis
            )
        )
    }
}

/**
 * Copies last month's budgets into [period] when [period] has none. Returns how many were copied.
 * Unused money never carries over: a copy is a new limit, not a balance.
 */
class CopyBudgetsFromPreviousMonth @Inject constructor(private val budgets: BudgetRepo) {
    suspend operator fun invoke(
        period: MonthPeriod,
        today: LocalDate,
        zone: ZoneId = ZoneId.systemDefault()
    ): Int {
        require(BudgetRules.isEditable(period, today)) { "Past months are read-only" }
        if (budgets.getForPeriod(period).isNotEmpty()) return 0
        val source = budgets.getForPeriod(MonthPeriods.previous(period, zone))
        source.forEach { previous ->
            budgets.save(
                previous.copy(
                    id = 0,
                    startDate = period.startMillis,
                    endDate = period.endMillis
                )
            )
        }
        return source.size
    }
}

class HideBudget @Inject constructor(private val budgets: BudgetRepo) {
    suspend operator fun invoke(period: MonthPeriod, today: LocalDate, budgetId: Long) {
        require(BudgetRules.isEditable(period, today)) { "Past months are read-only" }
        budgets.deactivate(budgetId)
    }
}

/**
 * Suggests a limit per category: the average monthly spend over the three months before [period],
 * rounded by [BudgetRules.roundSuggestion]. Categories with no spend are left out.
 */
class SuggestBudgets @Inject constructor(private val transactions: TransactionRepo) {
    suspend operator fun invoke(
        period: MonthPeriod,
        zone: ZoneId = ZoneId.systemDefault()
    ): Map<Long, Long> {
        val months = generateSequence(MonthPeriods.previous(period, zone)) { MonthPeriods.previous(it, zone) }
            .take(3)
            .toList()
        val history = transactions.observeBetween(months.last().startMillis, period.startMillis).first()
        val totals = mutableMapOf<Long, Long>()
        months.forEach { month ->
            SpendAnalytics.spendByCategory(history, month).forEach { total ->
                totals[total.categoryId] = (totals[total.categoryId] ?: 0L) + total.amount
            }
        }
        return totals
            .mapValues { (_, sum) -> BudgetRules.roundSuggestion(sum / months.size) }
            .filterValues { it > 0 }
    }
}
