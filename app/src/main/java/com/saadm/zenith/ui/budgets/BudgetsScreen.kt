package com.saadm.zenith.ui.budgets

import com.saadm.zenith.ui.components.charts.Gauge
import com.saadm.zenith.ui.components.Lu
import com.saadm.zenith.ui.components.categoryIcon
import com.saadm.zenith.ui.components.BudgetTile
import com.saadm.zenith.ui.common.LocalBottomBarInset
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Spacer
import com.saadm.zenith.ui.components.TotalAmount
import android.view.HapticFeedbackConstants
import androidx.compose.ui.platform.LocalView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.saadm.zenith.data.entity.CategoryEntity
import com.saadm.zenith.data.entity.TxnType
import com.saadm.zenith.domain.budget.BudgetLine
import com.saadm.zenith.domain.budget.BudgetRules
import com.saadm.zenith.domain.budget.BudgetStatus
import com.saadm.zenith.domain.budget.MonthBudgets
import com.saadm.zenith.domain.model.MoneyFormatter
import com.saadm.zenith.domain.model.MonthPeriods
import com.saadm.zenith.ui.common.PalettePreview
import com.saadm.zenith.ui.common.ScreenHeader
import com.saadm.zenith.ui.common.monthName
import com.saadm.zenith.ui.common.shortMonthName
import com.saadm.zenith.ui.components.AmountField
import com.saadm.zenith.ui.components.AmountText
import com.saadm.zenith.ui.components.BudgetLineRow
import com.saadm.zenith.ui.components.FilterChip
import com.saadm.zenith.ui.components.ListRow
import com.saadm.zenith.ui.components.MonthSwitcher
import com.saadm.zenith.ui.components.PrimaryButton
import com.saadm.zenith.ui.components.ProgressTrack
import com.saadm.zenith.ui.components.SecondaryButton
import com.saadm.zenith.ui.components.StatusTag
import com.saadm.zenith.ui.components.StatusTone
import com.saadm.zenith.ui.components.ZenithBottomSheet
import com.saadm.zenith.ui.components.ZenithCard
import com.saadm.zenith.ui.components.ZenithTextButton
import com.saadm.zenith.ui.theme.ZenithSpacing
import com.saadm.zenith.ui.theme.ZenithType
import com.saadm.zenith.ui.theme.zenithColors
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.abs

private const val SUGGESTIONS_SHOWN = 3

/** The budget sheet: [categoryId] null means a new budget, category still to be picked. */
private data class Editing(val categoryId: Long?, val limit: Long?)

@Composable
fun BudgetsScreen(onBack: () -> Unit, viewModel: BudgetsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    var editing by remember { mutableStateOf<Editing?>(null) }
    var pickingMonth by rememberSaveable { mutableStateOf(false) }
    val view = LocalView.current

    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Budgets", onBack) {
            if (state?.budgets?.editable == true) {
                FilterChip("New", false, { editing = Editing(null, null) }, icon = Lu.lucide_ic_plus)
            }
        }
        state?.let { loaded ->
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(ZenithSpacing.space4)
                    .padding(bottom = LocalBottomBarInset.current),
                verticalArrangement = Arrangement.spacedBy(ZenithSpacing.space3),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                MonthSwitcher(
                    label = "${loaded.period.startDate.shortMonthName()} ${loaded.period.startDate.year}",
                    onPrevious = viewModel::previousMonth,
                    onNext = viewModel::nextMonth,
                    nextEnabled = !loaded.isCurrentMonth,
                    onLabelClick = { pickingMonth = true }
                )
                BudgetsContent(
                    state = loaded,
                    onEdit = { categoryId, limit -> editing = Editing(categoryId, limit) },
                    onUseSuggestions = viewModel::useSuggestions,
                    onCopyLastMonth = viewModel::copyLastMonth
                )
            }
        }
    }

    val loaded = state ?: return
    editing?.let { target ->
        BudgetSheet(
            target = target,
            categories = loaded.expenseCategories.filter { category ->
                loaded.budgets.categories.none { it.categoryId == category.id }
            },
            names = loaded.names,
            onSave = { categoryId, limit ->
                viewModel.save(categoryId, limit) { view.performHapticFeedback(HapticFeedbackConstants.CONFIRM) }
                editing = null
            },
            onRemove = { categoryId ->
                viewModel.remove(categoryId)
                editing = null
            },
            onDismiss = { editing = null }
        )
    }
    if (pickingMonth) {
        MonthPickerSheet(
            history = loaded.history,
            onPick = {
                viewModel.showMonth(it)
                pickingMonth = false
            },
            onDismiss = { pickingMonth = false }
        )
    }
}

@Composable
private fun BudgetsContent(
    state: BudgetsState,
    onEdit: (categoryId: Long?, limit: Long?) -> Unit,
    onUseSuggestions: () -> Unit,
    onCopyLastMonth: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = zenithColors()
    val budgets = state.budgets
    val summary = BudgetSummary.of(budgets)
    Column(modifier, verticalArrangement = Arrangement.spacedBy(ZenithSpacing.space2)) {
        if (summary == null) {
            if (budgets.editable) {
                NoBudgets(state, { onEdit(null, null) }, onUseSuggestions, onCopyLastMonth)
            } else {
                ZenithCard {
                    Text("No budgets were set for this month.", style = ZenithType.body, color = colors.inkMuted)
                }
            }
            return@Column
        }

        Summary(state, summary)
        if (!budgets.editable) {
            Text("Past months are read-only.", style = ZenithType.caption, color = colors.inkMuted)
        }
        // The same line stays on a tile while the screen is up; it changes with the month and the next launch.
        val quipKey = sessionQuipSalt + state.period.startDate.monthValue
        // One tile per row at large font sizes, otherwise two.
        val columns = if (LocalDensity.current.fontScale >= 1.3f) 1 else 2
        val daysLeft = ChronoUnit.DAYS.between(state.today, state.period.endDate).takeIf { state.isCurrentMonth }
        // Over and nearly-over first.
        budgets.categories.filter { it.categoryId != null }.sortedBy { it.status == BudgetStatus.ON_TRACK }
            .chunked(columns)
            .forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max),
                    horizontalArrangement = Arrangement.spacedBy(ZenithSpacing.space3)
                ) {
                    row.forEach { line ->
                        val id = line.categoryId ?: return@forEach
                        val category = state.expenseCategories.firstOrNull { it.id == id }
                        BudgetTile(
                            name = state.names[id] ?: "Uncategorized",
                            icon = categoryIcon(category?.emoji)?.image,
                            line = line,
                            pace = budgets.pace.takeIf { state.isCurrentMonth },
                            daysLeft = daysLeft,
                            quip = budgetQuip(line.status, quipKey + id.toInt(), MoneyFormatter.format(abs(line.left)).text),
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                            onClick = if (budgets.editable) ({ onEdit(id, line.limit) }) else null
                        )
                    }
                    if (row.size < columns) Spacer(Modifier.weight(1f))
                }
            }
    }
}

@Composable
private fun Summary(state: BudgetsState, summary: BudgetSummary) {
    val colors = zenithColors()
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        val ratio = if (summary.limit > 0) summary.spent * 100 / summary.limit else 0
        Text("Overall spent: $ratio%", style = ZenithType.caption, color = colors.inkMuted)
        Gauge(
            spent = summary.spent,
            limit = summary.limit,
            paceFraction = if (state.isCurrentMonth) state.budgets.pace else 1f
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                TotalAmount(summary.left)
                Text(
                    if (state.isCurrentMonth) "left this month" else "left at the end",
                    style = ZenithType.caption,
                    color = colors.inkMuted
                )
            }
        }
        Text(
            "${MoneyFormatter.format(summary.spent).text} of ${MoneyFormatter.format(summary.limit).text}",
            style = ZenithType.caption,
            color = colors.inkMuted,
            modifier = Modifier.padding(vertical = ZenithSpacing.space2)
        )
        val overall = when (summary.status) {
            "Over budget" -> BudgetStatus.OVER
            "Ahead of pace" -> BudgetStatus.NEARING
            else -> BudgetStatus.ON_TRACK
        }
        StatusTag(
            budgetQuip(overall, sessionQuipSalt + state.period.startDate.monthValue, MoneyFormatter.format(abs(summary.left)).text),
            when (overall) {
                BudgetStatus.OVER -> StatusTone.Negative
                BudgetStatus.NEARING -> StatusTone.Caution
                BudgetStatus.ON_TRACK -> StatusTone.Positive
            }
        )
        if (state.isCurrentMonth) {
            val daysLeft = ChronoUnit.DAYS.between(state.today, state.period.endDate)
            val perDay = BudgetRules.perDayAllowance(summary.left, state.period, state.today) ?: 0L
            Text(
                "$daysLeft days left · about ${MoneyFormatter.format(perDay / 100 * 100).text} a day",
                style = ZenithType.bodyStrong,
                color = colors.ink,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = ZenithSpacing.space2)
            )
            Text(
                "${MoneyFormatter.format(abs(summary.underPace) / 100 * 100).text} " +
                    "${if (summary.underPace >= 0) "under" else "over"} an even pace",
                style = ZenithType.caption,
                color = colors.inkMuted
            )
        }
    }
}

@Composable
private fun NoBudgets(
    state: BudgetsState,
    onStartFromScratch: () -> Unit,
    onUseSuggestions: () -> Unit,
    onCopyLastMonth: () -> Unit
) {
    val colors = zenithColors()
    ZenithCard {
        Text("Set your first budget", style = ZenithType.heading, color = colors.ink)
    }
    if (state.canCopyLastMonth) {
        PrimaryButton("Copy last month's budgets", onCopyLastMonth, Modifier.fillMaxWidth())
    }
    if (state.suggestions.isNotEmpty()) {
        ZenithCard {
            Text("Suggested start", style = ZenithType.subheading, color = colors.ink)
            Text(
                "${MoneyFormatter.format(state.suggestions.values.sum()).text} a month, from your last 3 months.",
                style = ZenithType.caption,
                color = colors.inkMuted
            )
            val sorted = state.suggestions.entries.sortedByDescending { it.value }
            sorted.take(SUGGESTIONS_SHOWN).forEach { (categoryId, limit) ->
                ListRow(title = state.names[categoryId] ?: "Uncategorized", trailing = { AmountText(limit) })
            }
            if (sorted.size > SUGGESTIONS_SHOWN) {
                Text("+ ${sorted.size - SUGGESTIONS_SHOWN} more categories", style = ZenithType.caption, color = colors.inkMuted)
            }
            PrimaryButton("Use suggested budgets", onUseSuggestions, Modifier.fillMaxWidth())
        }
    }
    ZenithTextButton("Start from scratch", onStartFromScratch, Modifier.fillMaxWidth())
    ZenithCard {
        Column(verticalArrangement = Arrangement.spacedBy(ZenithSpacing.space2)) {
            StatusTag("Safe: under 85% of the limit", StatusTone.Positive)
            StatusTag("Nearing limit: 85–100%", StatusTone.Caution)
            StatusTag("Past the limit", StatusTone.Negative)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BudgetSheet(
    target: Editing,
    categories: List<CategoryEntity>,
    names: Map<Long, String>,
    onSave: (Long, Long) -> Unit,
    onRemove: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = zenithColors()
    var categoryId by remember(target) { mutableStateOf(target.categoryId) }
    var amount by remember(target) { mutableStateOf(target.limit?.let(MoneyFormatter::plain).orEmpty()) }
    val limit = MoneyFormatter.parse(amount)
    ZenithBottomSheet(onDismiss) {
        val existing = target.categoryId
        if (existing != null) {
            Text("Edit ${names[existing] ?: "budget"}", style = ZenithType.subheading, color = colors.ink)
        } else {
            Text("Add budget", style = ZenithType.subheading, color = colors.ink)
            if (categories.isEmpty()) {
                Text("Every category already has a budget.", style = ZenithType.body, color = colors.inkMuted)
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(ZenithSpacing.space2),
                verticalArrangement = Arrangement.spacedBy(ZenithSpacing.space2)
            ) {
                categories.forEach { category ->
                    FilterChip(category.name, categoryId == category.id, { categoryId = category.id })
                }
            }
        }
        AmountField(amount, { amount = it }, label = "Monthly limit")
        PrimaryButton(
            "Save budget",
            onClick = { categoryId?.let { id -> limit?.let { onSave(id, it) } } },
            modifier = Modifier.fillMaxWidth(),
            enabled = categoryId != null && limit != null
        )
        if (existing != null) {
            ZenithTextButton("Remove budget", { onRemove(existing) }, Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun MonthPickerSheet(history: List<MonthBudgets>, onPick: (Int) -> Unit, onDismiss: () -> Unit) {
    val colors = zenithColors()
    ZenithBottomSheet(onDismiss) {
        Text("Choose month", style = ZenithType.subheading, color = colors.ink)
        history.forEachIndexed { back, month ->
            val summary = BudgetSummary.of(month)
            val start = month.period.startDate
            ListRow(
                title = "${start.monthName()} ${start.year}" + if (back == 0) " · Current" else "",
                subtitle = if (summary == null) {
                    "No budgets set"
                } else {
                    "${MoneyFormatter.format(summary.spent).text} of ${MoneyFormatter.format(summary.limit).text}"
                },
                trailingCaption = when {
                    summary == null -> if (month.editable) "Add" else null
                    back == 0 -> summary.status
                    summary.spent > summary.limit -> "Over budget"
                    else -> "Within budget"
                },
                onClick = { onPick(back) }
            )
        }
    }
}

@Preview
@Composable
internal fun BudgetsPreview() {
    val today = LocalDate.of(2026, 10, 20)
    val period = MonthPeriods.containing(today)
    val budgets = MonthBudgets(
        period, null,
        listOf(
            BudgetLine(1, 600_000, 684_000, BudgetStatus.OVER),
            BudgetLine(2, 300_000, 100_000, BudgetStatus.ON_TRACK)
        ),
        0.65f, null, null, true
    )
    val categories = listOf("Food", "Transport").mapIndexed { i, name ->
        CategoryEntity(i + 1L, name, "", "#000000", TxnType.EXPENSE, i)
    }
    val state = BudgetsState(today, period, budgets, listOf(budgets), categories, emptyMap())
    val empty = state.copy(
        budgets = budgets.copy(categories = emptyList()),
        suggestions = mapOf(1L to 600_000L, 2L to 300_000L)
    )
    PalettePreview {
        Column { BudgetsContent(state, { _, _ -> }, {}, {}) }
        Column { BudgetsContent(empty, { _, _ -> }, {}, {}) }
    }
}
