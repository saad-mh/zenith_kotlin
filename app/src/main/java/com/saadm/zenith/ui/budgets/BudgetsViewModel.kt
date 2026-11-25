package com.saadm.zenith.ui.budgets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.saadm.zenith.data.entity.CategoryEntity
import com.saadm.zenith.data.entity.TxnType
import com.saadm.zenith.data.preferences.SettingsRepository
import com.saadm.zenith.domain.budget.BudgetRules
import com.saadm.zenith.domain.budget.MonthBudgets
import com.saadm.zenith.domain.model.MonthPeriod
import com.saadm.zenith.domain.model.MonthPeriods
import com.saadm.zenith.domain.repository.BudgetRepo
import com.saadm.zenith.domain.repository.CategoryRepo
import com.saadm.zenith.domain.usecase.CopyBudgetsFromPreviousMonth
import com.saadm.zenith.domain.usecase.HideBudget
import com.saadm.zenith.domain.usecase.ObserveMonthBudgets
import com.saadm.zenith.domain.usecase.SaveBudget
import com.saadm.zenith.domain.usecase.SuggestBudgets
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

private const val HISTORY_MONTHS = 6

/** The month as one figure: the overall budget if there is one, else the category budgets added up. */
data class BudgetSummary(val limit: Long, val spent: Long, val pace: Float) {
    val left: Long get() = limit - spent

    /** Positive while spending is behind an even pace. */
    val underPace: Long get() = (limit * pace.toDouble()).toLong() - spent

    val status: String
        get() = when {
            spent > limit -> "Over budget"
            underPace < 0 -> "Ahead of pace"
            else -> "On pace"
        }

    companion object {
        fun of(budgets: MonthBudgets): BudgetSummary? {
            val overall = budgets.overall
            if (overall == null && budgets.categories.isEmpty()) return null
            return BudgetSummary(
                limit = overall?.limit ?: budgets.categories.sumOf { it.limit },
                spent = overall?.spent ?: budgets.categories.sumOf { it.spent },
                pace = budgets.pace
            )
        }
    }
}

data class BudgetsState(
    val today: LocalDate,
    val current: MonthPeriod,
    val budgets: MonthBudgets,
    /** Newest first, starting at the current month. */
    val history: List<MonthBudgets>,
    /** Every category, so names resolve; [expenseCategories] are the ones that can be budgeted. */
    val categories: List<CategoryEntity>,
    /** Category id to suggested limit, filled only while this month has no budgets. */
    val suggestions: Map<Long, Long>
) {
    val period: MonthPeriod get() = budgets.period
    val isCurrentMonth: Boolean get() = period == current
    val names: Map<Long, String> get() = categories.associate { it.id to it.name }
    val expenseCategories: List<CategoryEntity>
        get() = categories.filter { it.txnType == TxnType.EXPENSE && !it.isDeleted }
    val canCopyLastMonth: Boolean
        get() = isCurrentMonth && history.getOrNull(1)?.let { it.categories.isNotEmpty() || it.overall != null } == true
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class BudgetsViewModel @Inject constructor(
    settings: SettingsRepository,
    private val budgetRepo: BudgetRepo,
    categoryRepo: CategoryRepo,
    observeBudgets: ObserveMonthBudgets,
    suggestBudgets: SuggestBudgets,
    private val saveBudget: SaveBudget,
    private val hideBudget: HideBudget,
    private val copyBudgets: CopyBudgetsFromPreviousMonth
) : ViewModel() {

    private val today = LocalDate.now()
    private val monthsBack = MutableStateFlow(0)

    val state: StateFlow<BudgetsState?> =
        settings.currentMonth(today).flatMapLatest { current ->
            val periods = generateSequence(current) { MonthPeriods.previous(it) }.take(HISTORY_MONTHS).toList()
            val history = combine(periods.map { observeBudgets(it, today) }) { it.toList() }
            combine(history, categoryRepo.observeAll(), monthsBack) { months, categories, back ->
                val shown = months[back.coerceIn(months.indices)]
                val empty = shown.categories.isEmpty() && shown.overall == null
                BudgetsState(
                    today = today,
                    current = current,
                    budgets = shown,
                    history = months,
                    categories = categories,
                    suggestions = if (empty && shown.editable) suggestBudgets(current) else emptyMap()
                )
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun showMonth(back: Int) {
        monthsBack.value = back.coerceIn(0, HISTORY_MONTHS - 1)
    }

    fun previousMonth() = showMonth(monthsBack.value + 1)

    fun nextMonth() = showMonth(monthsBack.value - 1)

    fun save(categoryId: Long, limit: Long) {
        val state = state.value ?: return
        if (!BudgetRules.isEditable(state.period, today) || limit <= 0) return
        viewModelScope.launch { saveBudget(state.period, today, categoryId, limit) }
    }

    fun remove(categoryId: Long) {
        val state = state.value ?: return
        if (!state.budgets.editable) return
        viewModelScope.launch {
            budgetRepo.getForPeriod(state.period).firstOrNull { it.categoryId == categoryId }
                ?.let { hideBudget(state.period, today, it.id) }
        }
    }

    fun useSuggestions() {
        val state = state.value ?: return
        if (!state.budgets.editable) return
        viewModelScope.launch {
            state.suggestions.forEach { (categoryId, limit) -> saveBudget(state.period, today, categoryId, limit) }
        }
    }

    fun copyLastMonth() {
        val state = state.value ?: return
        if (!state.budgets.editable) return
        viewModelScope.launch { copyBudgets(state.period, today) }
    }
}
