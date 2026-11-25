package com.saadm.zenith.ui.transactions

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.saadm.zenith.data.entity.TransactionEntity
import com.saadm.zenith.data.entity.TxnType
import com.saadm.zenith.domain.model.MonthPeriods
import com.saadm.zenith.domain.model.TxnFilter
import com.saadm.zenith.domain.transaction.TxnItems
import com.saadm.zenith.domain.transaction.TxnNames
import com.saadm.zenith.domain.transaction.dayLabel
import com.saadm.zenith.ui.common.PalettePreview
import com.saadm.zenith.ui.common.ScreenHeader
import com.saadm.zenith.ui.common.monthName
import com.saadm.zenith.ui.components.AmountText
import com.saadm.zenith.ui.components.EmptyState
import com.saadm.zenith.ui.components.FilterChip
import com.saadm.zenith.ui.components.MonthSwitcher
import com.saadm.zenith.ui.components.TxnItemRow
import com.saadm.zenith.ui.components.ZenithCard
import com.saadm.zenith.ui.theme.ZenithSpacing
import com.saadm.zenith.ui.theme.ZenithType
import com.saadm.zenith.ui.theme.zenithColors
import java.time.LocalDate

private val filterLabels = listOf(
    TxnFilter.ALL to "All",
    TxnFilter.INCOME to "Income",
    TxnFilter.EXPENSE to "Expense",
    TxnFilter.YOU_OWE to "You owe",
    TxnFilter.OWES_YOU to "They owe",
    TxnFilter.SETTLEMENTS to "Settlements"
)

@Composable
fun TransactionsScreen(
    onBack: () -> Unit,
    onAdd: () -> Unit,
    onEdit: (Long) -> Unit,
    onRepeat: (Long) -> Unit,
    viewModel: TxnListViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    var openId by remember { mutableStateOf<Long?>(null) }
    val loaded = state

    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Transactions", onBack) {
            val searching = loaded?.searching == true
            IconButton(onClick = { viewModel.setQuery(if (searching) null else "") }) {
                Icon(
                    if (searching) Icons.Rounded.Close else Icons.Rounded.Search,
                    contentDescription = if (searching) "Close search" else "Search transactions",
                    tint = zenithColors().ink
                )
            }
        }
        if (loaded != null) {
            TxnListContent(
                state = loaded,
                onPrevious = viewModel::previousMonth,
                onNext = viewModel::nextMonth,
                onFilter = viewModel::setFilter,
                onQuery = viewModel::setQuery,
                onSearchAllMonths = viewModel::searchAllMonths,
                onOpen = { openId = it },
                onAdd = onAdd
            )
        }
    }

    // Looked up by id, so the sheet follows changes and closes once the row is gone.
    val open = loaded?.days?.flatMap { it.items }?.firstOrNull { it.txn.id == openId }
    if (loaded != null && open != null) {
        TxnDetailSheet(
            item = open,
            names = loaded.names,
            today = loaded.today,
            onEdit = {
                openId = null
                onEdit(open.txn.id)
            },
            onRepeat = {
                openId = null
                onRepeat(open.txn.id)
            },
            onDelete = {
                viewModel.delete(open)
                openId = null
            },
            onDismiss = { openId = null }
        )
    }
}

@Composable
private fun TxnListContent(
    state: TxnListState,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onFilter: (TxnFilter) -> Unit,
    onQuery: (String) -> Unit,
    onSearchAllMonths: () -> Unit,
    onOpen: (Long) -> Unit,
    onAdd: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = zenithColors()
    val start = state.period.startDate
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(ZenithSpacing.space4),
        verticalArrangement = Arrangement.spacedBy(ZenithSpacing.space2)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(ZenithSpacing.space2)) {
                state.query.query?.let { text ->
                    OutlinedTextField(
                        value = text,
                        onValueChange = onQuery,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Search note, category or person") },
                        singleLine = true
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        if (state.query.allMonths) {
                            Text("All months", style = ZenithType.bodyStrong, color = colors.ink)
                        } else {
                            MonthSwitcher(
                                label = "${start.monthName()} ${start.year}",
                                onPrevious = onPrevious,
                                onNext = onNext,
                                nextEnabled = state.query.monthsBack > 0
                            )
                        }
                    }
                    Text(
                        if (state.count == 1) "1 transaction" else "${state.count} transactions",
                        style = ZenithType.caption,
                        color = colors.inkMuted
                    )
                }
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(ZenithSpacing.space2)
                ) {
                    filterLabels.forEach { (filter, label) ->
                        FilterChip(label, state.query.filter == filter, { onFilter(filter) })
                    }
                }
            }
        }

        if (state.days.isEmpty()) {
            item {
                val text = state.query.query.orEmpty().trim()
                when {
                    text.isNotEmpty() -> EmptyState(
                        title = "No results",
                        body = if (state.query.allMonths) "Nothing matches \"$text\"."
                        else "Nothing in ${start.monthName()} matches \"$text\".",
                        actionLabel = if (state.query.allMonths) null else "Search all months",
                        onAction = onSearchAllMonths
                    )
                    !state.hasAnyEntry -> EmptyState(
                        title = "No transactions yet",
                        body = "Add an expense, income or money a friend owes you. Everything stays on this device.",
                        actionLabel = "Add transaction",
                        onAction = onAdd
                    )
                    else -> EmptyState(
                        title = "Nothing here",
                        body = "No entries for this month and filter."
                    )
                }
            }
        }

        items(state.days, key = { it.date.toEpochDay() }) { day ->
            Column {
                Row(Modifier.padding(horizontal = ZenithSpacing.space1)) {
                    Text(
                        dayLabel(day.date, state.today),
                        style = ZenithType.bodyStrong,
                        color = colors.ink,
                        modifier = Modifier.weight(1f)
                    )
                    day.total?.let { AmountText(it, signed = true, style = ZenithType.caption) }
                }
                ZenithCard {
                    day.items.forEach { item -> TxnItemRow(item, onClick = { onOpen(item.txn.id) }) }
                }
            }
        }
    }
}

@Preview
@Composable
private fun TxnListPreview() {
    val today = LocalDate.of(2026, 10, 20)
    val rows = listOf(
        TransactionEntity(
            id = 1, amount = 42_000, type = TxnType.EXPENSE, transactedAt = 0, createdAt = 0, updatedAt = 0,
            categoryId = 1, payeeId = null, accountId = null, note = "Third Wave Coffee", receiptUri = null
        )
    )
    val items = TxnItems.of(rows, TxnNames(mapOf(1L to "Food")))
    val state = TxnListState(
        today = today,
        period = MonthPeriods.containing(today),
        query = TxnQuery(),
        days = listOf(TxnDay(today, -42_000, items)),
        names = TxnNames(),
        hasAnyEntry = true
    )
    PalettePreview {
        TxnListContent(state, {}, {}, {}, {}, {}, {}, {}, Modifier.height(420.dp))
        TxnListContent(state.copy(days = emptyList(), hasAnyEntry = false), {}, {}, {}, {}, {}, {}, {}, Modifier.height(420.dp))
    }
}
