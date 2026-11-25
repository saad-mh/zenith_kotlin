package com.saadm.zenith.domain.budget

import com.saadm.zenith.data.entity.BudgetEntity
import com.saadm.zenith.data.entity.TransactionEntity
import com.saadm.zenith.data.entity.TxnType
import com.saadm.zenith.domain.model.MonthPeriod
import java.time.LocalDate

enum class BudgetStatus { ON_TRACK, NEARING, OVER }

/** One budget against its spend for the month. [categoryId] is null for the overall budget. */
data class BudgetLine(
    val categoryId: Long?,
    val limit: Long,
    val spent: Long,
    val status: BudgetStatus
) {
    val left: Long get() = limit - spent
}

/**
 * A month of budgets. [pace] is the even-pace marker: the share of the month that has passed.
 * [perDayAllowance] is what is left spread over the days still to come, or null once the month is over.
 */
data class MonthBudgets(
    val period: MonthPeriod,
    val overall: BudgetLine?,
    val categories: List<BudgetLine>,
    val pace: Float,
    val leftToSpend: Long?,
    val perDayAllowance: Long?,
    val editable: Boolean
)

/** Budget rules as pure functions. Amounts are paise. */
object BudgetRules {

    /** Under 85% of the limit is on track; 85% to 100% is nearing; above 100% is over. */
    fun statusFor(spent: Long, limit: Long): BudgetStatus = when {
        spent > limit -> BudgetStatus.OVER
        spent * 100 >= limit * 85 -> BudgetStatus.NEARING
        else -> BudgetStatus.ON_TRACK
    }

    /** Day-of-period divided by days-in-period, from 0 before the month to 1 after it. */
    fun pace(period: MonthPeriod, today: LocalDate): Float = when {
        today < period.startDate -> 0f
        !today.isBefore(period.endDate) -> 1f
        else -> (today.toEpochDay() - period.startDate.toEpochDay() + 1).toFloat() / period.days
    }

    /** Past months are read-only. The current and future months can be changed. */
    fun isEditable(period: MonthPeriod, today: LocalDate): Boolean = today < period.endDate

    /** Left to spend spread over the days still to come, counting today. Null once the month is over. */
    fun perDayAllowance(left: Long, period: MonthPeriod, today: LocalDate): Long? {
        val remainingDays = when {
            today < period.startDate -> period.days.toLong()
            !today.isBefore(period.endDate) -> 0L
            else -> period.endDate.toEpochDay() - today.toEpochDay()
        }
        if (remainingDays == 0L) return null
        return if (left <= 0) 0L else left / remainingDays
    }

    /**
     * A suggested limit from an average spend. Rounds to the nearest ₹100 from ₹1,000 up, and to the
     * nearest ₹10 below that. Zero stays zero.
     */
    fun roundSuggestion(amount: Long): Long {
        if (amount <= 0) return 0
        val step = if (amount >= 1_000_00) 100_00L else 10_00L
        return (amount + step / 2) / step * step
    }

    /** Builds one month's budgets from its rows and its transactions. Only real spending counts. */
    fun summarize(
        period: MonthPeriod,
        budgets: List<BudgetEntity>,
        transactions: List<TransactionEntity>,
        today: LocalDate
    ): MonthBudgets {
        val inPeriod = transactions.filter { !it.isDeleted && it.transactedAt in period && it.type == TxnType.EXPENSE }
        val totalSpent = inPeriod.sumOf { it.amount }
        val spentByCategory = inPeriod.groupBy { it.categoryId }.mapValues { (_, items) -> items.sumOf { it.amount } }

        val lines = budgets.map { budget ->
            val spent = if (budget.categoryId == null) totalSpent else spentByCategory[budget.categoryId] ?: 0L
            BudgetLine(
                categoryId = budget.categoryId,
                limit = budget.limitAmount,
                spent = spent,
                status = statusFor(spent, budget.limitAmount)
            )
        }
        val overall = lines.firstOrNull { it.categoryId == null }
        val left = overall?.left
        return MonthBudgets(
            period = period,
            overall = overall,
            categories = lines.filter { it.categoryId != null },
            pace = pace(period, today),
            leftToSpend = left,
            perDayAllowance = left?.let { perDayAllowance(it, period, today) },
            editable = isEditable(period, today)
        )
    }
}
