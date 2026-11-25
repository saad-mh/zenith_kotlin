package com.saadm.zenith.domain.analytics

import com.saadm.zenith.data.entity.TransactionEntity
import com.saadm.zenith.data.entity.TxnType
import com.saadm.zenith.domain.model.MonthPeriod
import com.saadm.zenith.domain.model.MonthPeriods
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId

class SpendAnalyticsTest {

    private val zone: ZoneId = ZoneId.of("Asia/Kolkata")
    private val february = MonthPeriods.containing(LocalDate.of(2026, 2, 15), startDay = 1, zone = zone)
    private val january = MonthPeriods.previous(february, zone)

    private fun at(date: LocalDate): Long = date.atTime(12, 0).atZone(zone).toInstant().toEpochMilli()

    private fun txn(
        type: TxnType,
        amount: Long,
        date: LocalDate,
        categoryId: Long = 3L,
        isDeleted: Boolean = false
    ) = TransactionEntity(
        amount = amount,
        type = type,
        transactedAt = at(date),
        createdAt = at(date),
        updatedAt = at(date),
        categoryId = categoryId,
        payeeId = null,
        accountId = null,
        note = null,
        receiptUri = null,
        isDeleted = isDeleted
    )

    @Test
    fun summaryCountsOnlyIncomeAndExpense() {
        val items = listOf(
            txn(TxnType.INCOME, 5_000_000, LocalDate.of(2026, 2, 2)),
            txn(TxnType.EXPENSE, 120_000, LocalDate.of(2026, 2, 3)),
            txn(TxnType.DUE_TO, 900_000, LocalDate.of(2026, 2, 4)),
            txn(TxnType.DUE_FROM, 300_000, LocalDate.of(2026, 2, 5)),
            txn(TxnType.SETTLEMENT_FROM, 200_000, LocalDate.of(2026, 2, 6)),
            txn(TxnType.SETTLEMENT_TO, 100_000, LocalDate.of(2026, 2, 7)),
        )

        val summary = SpendAnalytics.summarize(items, february)

        assertEquals(5_000_000L, summary.income)
        assertEquals(120_000L, summary.expense)
        assertEquals(4_880_000L, summary.net)
    }

    @Test
    fun periodBoundariesAndDeletedRowsAreExcluded() {
        val items = listOf(
            txn(TxnType.EXPENSE, 100, LocalDate.of(2026, 1, 31)),
            txn(TxnType.EXPENSE, 200, LocalDate.of(2026, 2, 1)),
            txn(TxnType.EXPENSE, 400, LocalDate.of(2026, 2, 28)),
            txn(TxnType.EXPENSE, 800, LocalDate.of(2026, 3, 1)),
            txn(TxnType.EXPENSE, 1_600, LocalDate.of(2026, 2, 10), isDeleted = true),
        )

        assertEquals(600L, SpendAnalytics.summarize(items, february).expense)
    }

    @Test
    fun spendByCategoryIsLargestFirstAndExpenseOnly() {
        val items = listOf(
            txn(TxnType.EXPENSE, 300, LocalDate.of(2026, 2, 2), categoryId = 3L),
            txn(TxnType.EXPENSE, 900, LocalDate.of(2026, 2, 3), categoryId = 4L),
            txn(TxnType.EXPENSE, 100, LocalDate.of(2026, 2, 4), categoryId = 3L),
            txn(TxnType.INCOME, 5_000, LocalDate.of(2026, 2, 5), categoryId = 4L),
        )

        assertEquals(
            listOf(CategoryTotal(4L, 900), CategoryTotal(3L, 400)),
            SpendAnalytics.spendByCategory(items, february)
        )
    }

    @Test
    fun dayTotalsGroupByLocalDateNewestFirst() {
        val items = listOf(
            txn(TxnType.EXPENSE, 100, LocalDate.of(2026, 2, 2)),
            txn(TxnType.INCOME, 700, LocalDate.of(2026, 2, 2)),
            txn(TxnType.EXPENSE, 50, LocalDate.of(2026, 2, 9)),
            txn(TxnType.DUE_TO, 999, LocalDate.of(2026, 2, 9)),
        )

        assertEquals(
            listOf(DayTotal(LocalDate.of(2026, 2, 9), income = 0, expense = 50), DayTotal(LocalDate.of(2026, 2, 2), income = 700, expense = 100)),
            SpendAnalytics.dayTotals(items, february, zone)
        )
    }

    @Test
    fun cumulativeSpendRunsPerDayAgainstThePreviousMonth() {
        val items = listOf(
            txn(TxnType.EXPENSE, 100, LocalDate.of(2026, 1, 3)),
            txn(TxnType.EXPENSE, 50, LocalDate.of(2026, 2, 1)),
            txn(TxnType.EXPENSE, 25, LocalDate.of(2026, 2, 3)),
        )

        val cumulative = SpendAnalytics.cumulativeSpend(items, february, january, zone)

        assertEquals(february.days, cumulative.thisMonth.size)
        assertEquals(50L, cumulative.thisMonth[0])
        assertEquals(50L, cumulative.thisMonth[1])
        assertEquals(75L, cumulative.thisMonth[2])
        assertEquals(75L, cumulative.thisMonth.last())
        assertEquals(100L, cumulative.previousMonth.last())
    }

    @Test
    fun trendAveragesFiveEarlierMonthsAndProjectsAtTodaysPace() {
        val history = listOf(
            txn(TxnType.EXPENSE, 1_000, LocalDate.of(2025, 9, 10)),
            txn(TxnType.EXPENSE, 2_000, LocalDate.of(2025, 10, 10)),
            txn(TxnType.EXPENSE, 3_000, LocalDate.of(2025, 11, 10)),
            txn(TxnType.EXPENSE, 4_000, LocalDate.of(2025, 12, 10)),
            txn(TxnType.EXPENSE, 5_000, LocalDate.of(2026, 1, 10)),
            // 2026-02 has 28 days. By the 14th, 7000 has been spent.
            txn(TxnType.EXPENSE, 7_000, LocalDate.of(2026, 2, 5)),
        )

        val trend = SpendAnalytics.trend(history, february, today = LocalDate.of(2026, 2, 14), zone = zone)

        assertEquals(6, trend.months.size)
        assertEquals(7_000L, trend.months.last().expense)
        assertEquals(3_000L, trend.fiveMonthAverage) // (1000+2000+3000+4000+5000) / 5
        assertEquals(14_000L, trend.projection) // 7000 * 28 / 14
    }

    @Test
    fun projectionIsNullBeforeThePeriodAndActualAfterIt() {
        val spent = listOf(txn(TxnType.EXPENSE, 500, LocalDate.of(2026, 2, 2)))

        val before = SpendAnalytics.trend(spent, february, today = LocalDate.of(2026, 1, 20), zone = zone)
        val after = SpendAnalytics.trend(spent, february, today = LocalDate.of(2026, 4, 1), zone = zone)

        assertNull(before.projection)
        assertEquals(500L, after.projection)
    }

    @Test
    fun weeksStartOnTheChosenDayAndClipToThePeriod() {
        // 2026-02-01 is a Sunday. With Monday weeks the first week is clipped to start on the 1st.
        val items = listOf(
            txn(TxnType.EXPENSE, 100, LocalDate.of(2026, 2, 1), categoryId = 3L),
            txn(TxnType.EXPENSE, 200, LocalDate.of(2026, 2, 2), categoryId = 3L),
            txn(TxnType.EXPENSE, 400, LocalDate.of(2026, 2, 9), categoryId = 3L),
        )

        val monday = SpendAnalytics.weeklySpendByCategory(items, february, DayOfWeek.MONDAY, zone)
        val sunday = SpendAnalytics.weeklySpendByCategory(items, february, DayOfWeek.SUNDAY, zone)

        assertEquals(
            listOf(
                WeekCategorySpend(LocalDate.of(2026, 2, 1), 3L, 100),
                WeekCategorySpend(LocalDate.of(2026, 2, 2), 3L, 200),
                WeekCategorySpend(LocalDate.of(2026, 2, 9), 3L, 400),
            ),
            monday
        )
        // Sunday weeks: 1 and 2 Feb share the week that starts on Sunday 1 Feb; 9 Feb starts the 8th.
        assertEquals(
            listOf(
                WeekCategorySpend(LocalDate.of(2026, 2, 1), 3L, 300),
                WeekCategorySpend(LocalDate.of(2026, 2, 8), 3L, 400),
            ),
            sunday
        )
    }
}
