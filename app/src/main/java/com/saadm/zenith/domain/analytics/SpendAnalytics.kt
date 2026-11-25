package com.saadm.zenith.domain.analytics

import com.saadm.zenith.data.entity.TransactionEntity
import com.saadm.zenith.data.entity.TxnType
import com.saadm.zenith.domain.model.MonthPeriod
import com.saadm.zenith.domain.model.MonthPeriods
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import kotlin.math.roundToLong

// Pure functions over transactions, all amounts in paise. Only INCOME and EXPENSE count as
// income and spending: dues, settlements and transfers move balances but are not spending.

data class MonthSummary(val income: Long, val expense: Long) {
    val net: Long get() = income - expense
}

data class DayTotal(val date: LocalDate, val income: Long, val expense: Long)

data class CategoryTotal(val categoryId: Long, val amount: Long)

/** Running spend by day of the period. Index 0 is the first day; the list has one entry per day. */
data class CumulativeSpend(val thisMonth: List<Long>, val previousMonth: List<Long>)

data class MonthTotal(val period: MonthPeriod, val expense: Long)

/**
 * Six months ending with the current one, oldest first.
 * [fiveMonthAverage] is the mean of the five months before the current one.
 * [projection] is the current month's spend scaled to the full month at today's pace.
 */
data class MonthTrend(val months: List<MonthTotal>, val fiveMonthAverage: Long, val projection: Long?)

data class WeekCategorySpend(val weekStart: LocalDate, val categoryId: Long, val amount: Long)

object SpendAnalytics {

    fun summarize(transactions: List<TransactionEntity>, period: MonthPeriod): MonthSummary {
        val inPeriod = transactions.inPeriod(period)
        return MonthSummary(
            income = inPeriod.sumOfType(TxnType.INCOME),
            expense = inPeriod.sumOfType(TxnType.EXPENSE)
        )
    }

    fun dayTotals(
        transactions: List<TransactionEntity>,
        period: MonthPeriod,
        zone: ZoneId = ZoneId.systemDefault()
    ): List<DayTotal> {
        return transactions.inPeriod(period)
            .filter { it.type == TxnType.INCOME || it.type == TxnType.EXPENSE }
            .groupBy { it.transactedAt.toLocalDate(zone) }
            .map { (date, items) ->
                DayTotal(
                    date = date,
                    income = items.sumOfType(TxnType.INCOME),
                    expense = items.sumOfType(TxnType.EXPENSE)
                )
            }
            .sortedByDescending { it.date }
    }

    /** Expense by category for the period, largest first. */
    fun spendByCategory(transactions: List<TransactionEntity>, period: MonthPeriod): List<CategoryTotal> {
        return transactions.inPeriod(period)
            .filter { it.type == TxnType.EXPENSE }
            .groupBy { it.categoryId }
            .map { (categoryId, items) -> CategoryTotal(categoryId, items.sumOf { it.amount }) }
            .sortedByDescending { it.amount }
    }

    /** Running spend for this period and the one before it, each one entry per day. */
    fun cumulativeSpend(
        transactions: List<TransactionEntity>,
        period: MonthPeriod,
        previousPeriod: MonthPeriod,
        zone: ZoneId = ZoneId.systemDefault()
    ): CumulativeSpend = CumulativeSpend(
        thisMonth = runningTotals(transactions, period, zone),
        previousMonth = runningTotals(transactions, previousPeriod, zone)
    )

    /**
     * Six months ending with [endingPeriod], and a projection for it.
     * The projection uses the days elapsed up to [today]. It is null before the period starts,
     * and equal to the actual spend once the period is over.
     */
    fun trend(
        transactions: List<TransactionEntity>,
        endingPeriod: MonthPeriod,
        today: LocalDate,
        zone: ZoneId = ZoneId.systemDefault()
    ): MonthTrend {
        val periods = generateSequence(endingPeriod) { MonthPeriods.previous(it, zone) }
            .take(6)
            .toList()
            .reversed()
        val months = periods.map { period ->
            MonthTotal(period, transactions.inPeriod(period).sumOfType(TxnType.EXPENSE))
        }
        val earlier = months.dropLast(1).map { it.expense }
        val average = if (earlier.isEmpty()) 0L else (earlier.sum().toDouble() / earlier.size).roundToLong()

        val spent = months.last().expense
        val projection = when {
            today < endingPeriod.startDate -> null
            !today.isBefore(endingPeriod.endDate) -> spent
            else -> {
                val elapsed = (today.toEpochDay() - endingPeriod.startDate.toEpochDay() + 1).coerceIn(1, endingPeriod.days.toLong())
                (spent.toDouble() * endingPeriod.days / elapsed).roundToLong()
            }
        }
        return MonthTrend(months, average, projection)
    }

    /** Expense per category per week. Weeks start on [weekStart] and are clipped to the period. */
    fun weeklySpendByCategory(
        transactions: List<TransactionEntity>,
        period: MonthPeriod,
        weekStart: DayOfWeek,
        zone: ZoneId = ZoneId.systemDefault()
    ): List<WeekCategorySpend> {
        return transactions.inPeriod(period)
            .filter { it.type == TxnType.EXPENSE }
            .groupBy { txn ->
                val date = txn.transactedAt.toLocalDate(zone)
                val weekBegin = date.with(TemporalAdjusters.previousOrSame(weekStart))
                (if (weekBegin.isBefore(period.startDate)) period.startDate else weekBegin) to txn.categoryId
            }
            .map { (key, items) -> WeekCategorySpend(key.first, key.second, items.sumOf { it.amount }) }
            .sortedWith(compareBy({ it.weekStart }, { it.categoryId }))
    }

    private fun runningTotals(transactions: List<TransactionEntity>, period: MonthPeriod, zone: ZoneId): List<Long> {
        val daily = LongArray(period.days)
        transactions.inPeriod(period)
            .filter { it.type == TxnType.EXPENSE }
            .forEach { txn ->
                val index = (txn.transactedAt.toLocalDate(zone).toEpochDay() - period.startDate.toEpochDay()).toInt()
                daily[index] += txn.amount
            }
        var running = 0L
        return daily.map { running += it; running }
    }

    private fun List<TransactionEntity>.inPeriod(period: MonthPeriod): List<TransactionEntity> =
        filter { !it.isDeleted && it.transactedAt in period }

    private fun List<TransactionEntity>.sumOfType(type: TxnType): Long =
        filter { it.type == type }.sumOf { it.amount }

    private fun Long.toLocalDate(zone: ZoneId): LocalDate = Instant.ofEpochMilli(this).atZone(zone).toLocalDate()
}
