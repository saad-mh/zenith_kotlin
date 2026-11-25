package com.saadm.zenith.ui.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.saadm.zenith.data.entity.TxnType
import com.saadm.zenith.data.preferences.SettingsRepository
import com.saadm.zenith.domain.model.MonthPeriod
import com.saadm.zenith.domain.model.MonthPeriods
import com.saadm.zenith.domain.model.TxnFilter
import com.saadm.zenith.domain.repository.TransactionRepo
import com.saadm.zenith.domain.transaction.TxnItem
import com.saadm.zenith.domain.transaction.TxnItems
import com.saadm.zenith.domain.transaction.TxnNames
import com.saadm.zenith.domain.usecase.ObserveTxnNames
import com.saadm.zenith.domain.usecase.SearchTransactions
import com.saadm.zenith.ui.common.toLocalDate
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

/** What the user has asked to see. [query] is null while search is closed. */
data class TxnQuery(
    val monthsBack: Int = 0,
    val filter: TxnFilter = TxnFilter.ALL,
    val query: String? = null,
    val allMonths: Boolean = false
)

/** One day of the list. [total] is that day's income minus spending, or null when it has neither. */
data class TxnDay(val date: LocalDate, val total: Long?, val items: List<TxnItem>)

/** Spending and income across the rows on show. Dues, settlements and transfers count as neither. */
data class TxnTotals(val spent: Long, val income: Long) {
    companion object {
        fun of(days: List<TxnDay>): TxnTotals {
            val rows = days.flatMap { it.items }.map { it.txn }
            return TxnTotals(
                spent = rows.filter { it.type == TxnType.EXPENSE }.sumOf { it.amount },
                income = rows.filter { it.type == TxnType.INCOME }.sumOf { it.amount }
            )
        }
    }
}

data class TxnListState(
    val today: LocalDate,
    val period: MonthPeriod,
    val query: TxnQuery,
    val days: List<TxnDay>,
    val names: TxnNames,
    /** False only when nothing has ever been recorded. */
    val hasAnyEntry: Boolean
) {
    val count: Int get() = days.sumOf { it.items.size }
    val searching: Boolean get() = query.query != null

    /** True when a filter or search text is narrowing the list. */
    val filtered: Boolean get() = query.filter != TxnFilter.ALL || !query.query.isNullOrBlank()

    val totals: TxnTotals get() = TxnTotals.of(days)
}

/** A split matches a filter when the expense or any of its shares does. */
fun TxnFilter.matches(item: TxnItem): Boolean = matches(item.txn) || item.shares.any { matches(it) }

/**
 * Narrows [items] (newest first) and groups them by day. [matchIds] are the rows the search found;
 * pass null when there is no search text.
 */
fun groupByDay(
    items: List<TxnItem>,
    period: MonthPeriod,
    query: TxnQuery,
    matchIds: Set<Long>?
): List<TxnDay> {
    val everyMonth = matchIds != null && query.allMonths
    return items
        .filter { everyMonth || it.txn.transactedAt in period }
        .filter { query.filter.matches(it) }
        .filter { item -> matchIds == null || item.txn.id in matchIds || item.shares.any { it.id in matchIds } }
        .groupBy { it.txn.transactedAt.toLocalDate() }
        .map { (date, onDay) ->
            // Dues, settlements and transfers move balances but are not income or spending.
            val money = onDay.filter { it.txn.type == TxnType.INCOME || it.txn.type == TxnType.EXPENSE }
            TxnDay(date, if (money.isEmpty()) null else money.sumOf { it.signedAmount }, onDay)
        }
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class TxnListViewModel @Inject constructor(
    private val repository: TransactionRepo,
    settings: SettingsRepository,
    observeNames: ObserveTxnNames,
    search: SearchTransactions
) : ViewModel() {

    private val today = LocalDate.now()
    private val query = MutableStateFlow(TxnQuery())

    private val matches = query.map { it.query?.trim().orEmpty() }.flatMapLatest { text ->
        search(text).map { rows -> if (text.isEmpty()) null else rows.map { it.id }.toSet() }
    }

    val state: StateFlow<TxnListState?> = combine(
        repository.observeAllActive(),
        observeNames(),
        settings.currentMonth(today),
        query,
        matches
    ) { all, names, current, asked, matchIds ->
        val period = generateSequence(current) { MonthPeriods.previous(it) }.elementAt(asked.monthsBack)
        TxnListState(
            today = today,
            period = period,
            query = asked,
            days = groupByDay(TxnItems.of(all, names), period, asked, matchIds),
            names = names,
            hasAnyEntry = all.isNotEmpty()
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun previousMonth() = query.update { it.copy(monthsBack = it.monthsBack + 1) }

    fun nextMonth() = query.update { it.copy(monthsBack = (it.monthsBack - 1).coerceAtLeast(0)) }

    fun setFilter(filter: TxnFilter) = query.update { it.copy(filter = filter) }

    fun setQuery(text: String?) = query.update {
        it.copy(query = text, allMonths = it.allMonths && text != null)
    }

    /** Drops the filter and search text. The month stays. */
    fun clearFilters() = query.update { TxnQuery(monthsBack = it.monthsBack) }

    fun searchAllMonths() = query.update { it.copy(allMonths = true) }

    /** Permanent. A split goes with all of its shares. */
    fun delete(item: TxnItem) {
        viewModelScope.launch { repository.deleteAll(listOf(item.txn) + item.shares) }
    }
}
