package com.saadm.zenith.ui.categories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.saadm.zenith.data.entity.CategoryEntity
import com.saadm.zenith.data.entity.TransactionEntity
import com.saadm.zenith.data.entity.TxnType
import com.saadm.zenith.data.preferences.SettingsRepository
import com.saadm.zenith.domain.model.MonthPeriod
import com.saadm.zenith.domain.model.MonthPeriods
import com.saadm.zenith.domain.repository.CategoryRepo
import com.saadm.zenith.domain.repository.TransactionRepo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class CategoryRow(val category: CategoryEntity, val total: Long)

data class CategoriesState(
    val period: MonthPeriod,
    val isCurrentMonth: Boolean,
    val type: TxnType,
    /** The active categories of [type], largest total first. */
    val rows: List<CategoryRow>
)

/** Each active category of [type] with what the month put in it. */
fun categoryRows(
    categories: List<CategoryEntity>,
    transactions: List<TransactionEntity>,
    period: MonthPeriod,
    type: TxnType
): List<CategoryRow> {
    val totals = transactions
        .filter { !it.isDeleted && it.type == type && it.transactedAt in period }
        .groupBy { it.categoryId }
        .mapValues { (_, rows) -> rows.sumOf { it.amount } }
    return categories
        .filter { it.txnType == type && !it.isDeleted }
        .map { CategoryRow(it, totals[it.id] ?: 0L) }
        .sortedWith(compareByDescending<CategoryRow> { it.total }.thenBy { it.category.sortOrder })
}

/** Why [name] cannot be used, or null if it can. [selfId] is the category being renamed. */
fun nameProblem(name: String, existing: List<CategoryEntity>, selfId: Long?): String? {
    val wanted = name.trim()
    return when {
        wanted.isEmpty() -> "Enter a name"
        existing.any { it.id != selfId && it.name.equals(wanted, ignoreCase = true) } -> "That name is already used"
        else -> null
    }
}

@HiltViewModel
class CategoriesViewModel @Inject constructor(
    private val categories: CategoryRepo,
    transactions: TransactionRepo,
    settings: SettingsRepository
) : ViewModel() {

    private val today = LocalDate.now()
    private val view = MutableStateFlow(0 to TxnType.EXPENSE)

    val state: StateFlow<CategoriesState?> = combine(
        categories.observeAll(),
        transactions.observeAllActive(),
        settings.currentMonth(today),
        view
    ) { all, rows, current, (back, type) ->
        val period = generateSequence(current) { MonthPeriods.previous(it) }.elementAt(back)
        CategoriesState(period, back == 0, type, categoryRows(all, rows, period, type))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun previousMonth() = view.update { (back, type) -> back + 1 to type }

    fun nextMonth() = view.update { (back, type) -> (back - 1).coerceAtLeast(0) to type }

    fun show(type: TxnType) = view.update { (back, _) -> back to type }

    fun add(name: String) {
        val loaded = state.value ?: return
        if (nameProblem(name, loaded.rows.map { it.category }, null) != null) return
        val order = (loaded.rows.maxOfOrNull { it.category.sortOrder } ?: 0) + 1
        viewModelScope.launch {
            categories.save(
                CategoryEntity(
                    name = name.trim(), emoji = "", colorHex = "#777FCF", txnType = loaded.type, sortOrder = order
                )
            )
        }
    }

    fun rename(category: CategoryEntity, name: String) {
        val others = state.value?.rows?.map { it.category }.orEmpty()
        if (nameProblem(name, others, category.id) != null) return
        viewModelScope.launch { categories.save(category.copy(name = name.trim())) }
    }

    /** The default category stays, since entries without a choice are filed under it. */
    fun archive(category: CategoryEntity) {
        if (category.isDefault) return
        viewModelScope.launch { categories.archive(category.id) }
    }

    fun merge(from: CategoryEntity, into: CategoryEntity) {
        if (from.isDefault || from.id == into.id || from.txnType != into.txnType) return
        viewModelScope.launch { categories.merge(from.id, into.id) }
    }
}
