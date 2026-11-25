package com.saadm.zenith.ui.insights

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.saadm.zenith.data.preferences.SettingsRepository
import com.saadm.zenith.domain.analytics.CategoryTotal
import com.saadm.zenith.domain.analytics.MonthSummary
import com.saadm.zenith.domain.analytics.MonthTrend
import com.saadm.zenith.domain.budget.BudgetLine
import com.saadm.zenith.domain.budget.BudgetStatus
import com.saadm.zenith.domain.budget.MonthBudgets
import com.saadm.zenith.domain.model.MonthPeriod
import com.saadm.zenith.domain.model.MonthPeriods
import com.saadm.zenith.domain.payee.SharedBalanceSummary
import com.saadm.zenith.domain.transaction.TxnNames
import com.saadm.zenith.domain.usecase.ObserveMonthBudgets
import com.saadm.zenith.domain.usecase.ObserveMonthSummary
import com.saadm.zenith.domain.usecase.ObserveSharedBalances
import com.saadm.zenith.domain.usecase.ObserveSixMonthTrend
import com.saadm.zenith.domain.usecase.ObserveSpendByCategory
import com.saadm.zenith.domain.usecase.ObserveTxnNames
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.time.LocalDate
import javax.inject.Inject

private const val NAMED_CATEGORIES = 4

/** The largest categories by name, and the rest added up as "Everything else". */
fun topCategories(totals: List<CategoryTotal>, names: Map<Long, String>): List<Pair<String, Long>> {
    val sorted = totals.sortedByDescending { it.amount }
    // A single leftover keeps its own name rather than hiding behind "Everything else".
    val keep = if (sorted.size <= NAMED_CATEGORIES + 1) sorted.size else NAMED_CATEGORIES
    val named = sorted.take(keep).map { (names[it.categoryId] ?: "Uncategorized") to it.amount }
    val rest = sorted.drop(keep).sumOf { it.amount }
    return if (rest > 0) named + ("Everything else" to rest) else named
}

/** The two budgets that most need a look, worst first. */
fun worstBudgets(lines: List<BudgetLine>): List<BudgetLine> = lines
    .filter { it.status != BudgetStatus.ON_TRACK }
    .sortedByDescending { it.spent.toDouble() / it.limit }
    .take(2)

data class InsightsState(
    val today: LocalDate,
    val period: MonthPeriod,
    val isCurrentMonth: Boolean,
    val summary: MonthSummary,
    val budgets: MonthBudgets,
    val trend: MonthTrend,
    val categories: List<Pair<String, Long>>,
    val shared: SharedBalanceSummary,
    val names: TxnNames
) {
    /** Share of income not spent, or null with no income to compare against. */
    val keptPercent: Int?
        get() = if (summary.income <= 0) null else (summary.net * 100 / summary.income).toInt()
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class InsightsViewModel @Inject constructor(
    settings: SettingsRepository,
    observeSummary: ObserveMonthSummary,
    observeBudgets: ObserveMonthBudgets,
    observeTrend: ObserveSixMonthTrend,
    observeByCategory: ObserveSpendByCategory,
    observeShared: ObserveSharedBalances,
    observeNames: ObserveTxnNames
) : ViewModel() {

    private val today = LocalDate.now()
    private val monthsBack = MutableStateFlow(0)

    val state: StateFlow<InsightsState?> =
        combine(settings.currentMonth(today), monthsBack) { current, back ->
            generateSequence(current) { MonthPeriods.previous(it) }.elementAt(back) to back
        }.flatMapLatest { (period, back) ->
            val month = combine(
                observeSummary(period),
                observeBudgets(period, today),
                observeTrend(period, today),
                observeByCategory(period),
                ::Month
            )
            combine(month, observeShared(), observeNames()) { m, shared, names ->
                InsightsState(
                    today = today,
                    period = period,
                    isCurrentMonth = back == 0,
                    summary = m.summary,
                    budgets = m.budgets,
                    trend = m.trend,
                    categories = topCategories(m.byCategory, names.categories),
                    shared = shared,
                    names = names
                )
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun previousMonth() = monthsBack.update { it + 1 }

    fun nextMonth() = monthsBack.update { (it - 1).coerceAtLeast(0) }

    private data class Month(
        val summary: MonthSummary,
        val budgets: MonthBudgets,
        val trend: MonthTrend,
        val byCategory: List<CategoryTotal>
    )
}
