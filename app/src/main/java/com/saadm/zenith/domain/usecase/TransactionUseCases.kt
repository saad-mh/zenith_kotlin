package com.saadm.zenith.domain.usecase

import com.saadm.zenith.data.entity.TransactionEntity
import com.saadm.zenith.domain.analytics.CategoryTotal
import com.saadm.zenith.domain.analytics.CumulativeSpend
import com.saadm.zenith.domain.analytics.DayTotal
import com.saadm.zenith.domain.analytics.MonthSummary
import com.saadm.zenith.domain.analytics.MonthTrend
import com.saadm.zenith.domain.analytics.SpendAnalytics
import com.saadm.zenith.domain.analytics.WeekCategorySpend
import com.saadm.zenith.domain.model.MonthPeriod
import com.saadm.zenith.domain.model.MonthPeriods
import com.saadm.zenith.domain.model.TxnFilter
import com.saadm.zenith.domain.repository.TransactionRepo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

// Each use case reads one range from the repository and derives its answer. ViewModels call these
// and never compute totals themselves.

class ObserveTransactions @Inject constructor(private val repo: TransactionRepo) {
    operator fun invoke(period: MonthPeriod, filter: TxnFilter = TxnFilter.ALL): Flow<List<TransactionEntity>> =
        repo.observeBetween(period.startMillis, period.endMillis).map { items -> items.filter(filter::matches) }
}

class ObserveMonthSummary @Inject constructor(private val repo: TransactionRepo) {
    operator fun invoke(period: MonthPeriod): Flow<MonthSummary> =
        repo.observeBetween(period.startMillis, period.endMillis).map { SpendAnalytics.summarize(it, period) }
}

class ObserveDayTotals @Inject constructor(private val repo: TransactionRepo) {
    operator fun invoke(period: MonthPeriod, zone: ZoneId = ZoneId.systemDefault()): Flow<List<DayTotal>> =
        repo.observeBetween(period.startMillis, period.endMillis).map { SpendAnalytics.dayTotals(it, period, zone) }
}

class ObserveSpendByCategory @Inject constructor(private val repo: TransactionRepo) {
    operator fun invoke(period: MonthPeriod): Flow<List<CategoryTotal>> =
        repo.observeBetween(period.startMillis, period.endMillis).map { SpendAnalytics.spendByCategory(it, period) }
}

/** This month's running spend against the month before, one point per day. */
class ObserveCumulativeSpend @Inject constructor(private val repo: TransactionRepo) {
    operator fun invoke(period: MonthPeriod, zone: ZoneId = ZoneId.systemDefault()): Flow<CumulativeSpend> {
        val previous = MonthPeriods.previous(period, zone)
        return repo.observeBetween(previous.startMillis, period.endMillis).map { items ->
            SpendAnalytics.cumulativeSpend(items, period, previous, zone)
        }
    }
}

/** Six months ending at [endingPeriod], with the five-month average and a month-end projection as of [today]. */
class ObserveSixMonthTrend @Inject constructor(private val repo: TransactionRepo) {
    operator fun invoke(
        endingPeriod: MonthPeriod,
        today: LocalDate,
        zone: ZoneId = ZoneId.systemDefault()
    ): Flow<MonthTrend> {
        val oldest = generateSequence(endingPeriod) { MonthPeriods.previous(it, zone) }.take(6).last()
        return repo.observeBetween(oldest.startMillis, endingPeriod.endMillis).map { items ->
            SpendAnalytics.trend(items, endingPeriod, today, zone)
        }
    }
}

class ObserveWeeklySpendByCategory @Inject constructor(private val repo: TransactionRepo) {
    operator fun invoke(
        period: MonthPeriod,
        weekStart: DayOfWeek,
        zone: ZoneId = ZoneId.systemDefault()
    ): Flow<List<WeekCategorySpend>> =
        repo.observeBetween(period.startMillis, period.endMillis).map {
            SpendAnalytics.weeklySpendByCategory(it, period, weekStart, zone)
        }
}

class SearchTransactions @Inject constructor(private val repo: TransactionRepo) {
    operator fun invoke(query: String): Flow<List<TransactionEntity>> = repo.observeSearch(query)
}
