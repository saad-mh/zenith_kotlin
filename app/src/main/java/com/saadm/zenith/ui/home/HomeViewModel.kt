package com.saadm.zenith.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.saadm.zenith.data.preferences.SettingsRepository
import com.saadm.zenith.domain.account.NetPosition
import com.saadm.zenith.domain.analytics.CumulativeSpend
import com.saadm.zenith.domain.model.MonthPeriod
import com.saadm.zenith.domain.payee.BalanceDirection
import com.saadm.zenith.domain.payee.PayeeBalanceLine
import com.saadm.zenith.domain.payee.SharedBalanceSummary
import com.saadm.zenith.domain.repository.TransactionRepo
import com.saadm.zenith.domain.transaction.TxnItem
import com.saadm.zenith.domain.transaction.TxnItems
import com.saadm.zenith.domain.usecase.ObserveCumulativeSpend
import com.saadm.zenith.domain.usecase.ObserveNetPosition
import com.saadm.zenith.domain.usecase.ObserveSharedBalances
import com.saadm.zenith.domain.usecase.ObserveTxnNames
import com.saadm.zenith.ui.common.toLocalDate
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import javax.inject.Inject

/** Spending so far this month, and the month before it at the same day. */
data class MonthSpend(
    val period: MonthPeriod,
    val spent: Long,
    val lastAtSamePoint: Long,
    val thisSoFar: List<Long>,
    val lastMonth: List<Long>
) {
    /** Positive when this month is ahead of the last one. */
    val difference: Long get() = spent - lastAtSamePoint

    companion object {
        fun of(period: MonthPeriod, cumulative: CumulativeSpend, today: LocalDate): MonthSpend {
            val day = ChronoUnit.DAYS.between(period.startDate, today).toInt().coerceIn(0, period.days - 1)
            val soFar = cumulative.thisMonth.take(day + 1)
            val last = cumulative.previousMonth
            return MonthSpend(
                period = period,
                spent = soFar.lastOrNull() ?: 0L,
                lastAtSamePoint = last.getOrNull(day) ?: last.lastOrNull() ?: 0L,
                thisSoFar = soFar,
                lastMonth = last
            )
        }
    }
}

/** The oldest balance someone still owes the user. */
data class Nudge(val line: PayeeBalanceLine, val daysOpen: Long)

fun oldestNudge(shared: SharedBalanceSummary, today: LocalDate): Nudge? {
    val line = shared.oldestFirst.firstOrNull { it.direction == BalanceDirection.OWES_YOU } ?: return null
    val since = line.oldestOpenAt?.toLocalDate() ?: today
    return Nudge(line, ChronoUnit.DAYS.between(since, today).coerceAtLeast(0))
}

data class RecentTxn(val item: TxnItem, val date: LocalDate)

data class OverviewState(
    val today: LocalDate,
    val netPosition: NetPosition,
    val spend: MonthSpend,
    val shared: SharedBalanceSummary,
    val nudge: Nudge?,
    val recent: List<RecentTxn>,
    val profileName: String = ""
)

private const val RECENT_COUNT = 5

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    settings: SettingsRepository,
    transactions: TransactionRepo,
    observeNetPosition: ObserveNetPosition,
    observeSharedBalances: ObserveSharedBalances,
    observeCumulativeSpend: ObserveCumulativeSpend,
    observeNames: ObserveTxnNames
) : ViewModel() {

    private val today = LocalDate.now()

    private val spend = settings.currentMonth(today).flatMapLatest { period ->
        observeCumulativeSpend(period).map { MonthSpend.of(period, it, today) }
    }

    private val recent = combine(transactions.observeAllActive(), observeNames()) { all, names ->
        // Fold before counting, so a split counts as one of the five.
        TxnItems.of(all, names).take(RECENT_COUNT).map { RecentTxn(it, it.txn.transactedAt.toLocalDate()) }
    }

    /** Null until the first read, so the screen never shows a false zero. */
    val state: StateFlow<OverviewState?> =
        combine(observeNetPosition(), spend, observeSharedBalances(), recent, settings.settings) { net, month, shared, rows, preferences ->
            OverviewState(today, net, month, shared, oldestNudge(shared, today), rows, preferences.profileName)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}
