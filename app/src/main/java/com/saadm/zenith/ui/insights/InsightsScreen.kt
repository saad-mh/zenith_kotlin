package com.saadm.zenith.ui.insights

import com.saadm.zenith.ui.components.TotalAmount
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.saadm.zenith.domain.analytics.MonthSummary
import com.saadm.zenith.domain.analytics.MonthTotal
import com.saadm.zenith.domain.analytics.MonthTrend
import com.saadm.zenith.domain.budget.BudgetLine
import com.saadm.zenith.domain.budget.BudgetStatus
import com.saadm.zenith.domain.budget.MonthBudgets
import com.saadm.zenith.domain.model.MoneyFormatter
import com.saadm.zenith.domain.model.MonthPeriods
import com.saadm.zenith.domain.payee.PayeeBalanceLine
import com.saadm.zenith.domain.payee.SharedBalanceSummary
import com.saadm.zenith.domain.transaction.TxnNames
import com.saadm.zenith.domain.transaction.dayLabel
import com.saadm.zenith.ui.common.PalettePreview
import com.saadm.zenith.ui.common.monthName
import com.saadm.zenith.ui.common.shortMonthName
import com.saadm.zenith.ui.common.toLocalDate
import com.saadm.zenith.ui.components.AmountText
import com.saadm.zenith.ui.components.AmountTone
import com.saadm.zenith.ui.components.Avatar
import com.saadm.zenith.ui.components.BudgetLineRow
import com.saadm.zenith.ui.components.ListRow
import com.saadm.zenith.ui.components.MonthSwitcher
import com.saadm.zenith.ui.components.ProgressTrack
import com.saadm.zenith.ui.components.SecondaryButton
import com.saadm.zenith.ui.components.SectionHeader
import com.saadm.zenith.ui.components.ZenithCard
import com.saadm.zenith.ui.components.charts.MonthBar
import com.saadm.zenith.ui.components.charts.MonthlyBars
import com.saadm.zenith.ui.components.charts.ShareBar
import com.saadm.zenith.ui.components.charts.ShareSlice
import com.saadm.zenith.ui.theme.ZenithSpacing
import com.saadm.zenith.ui.theme.ZenithType
import com.saadm.zenith.ui.theme.zenithColors
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.abs

private const val OLDEST_SHOWN = 3

@Composable
fun InsightsScreen(
    onOpenBudgets: () -> Unit = {},
    onOpenPayees: () -> Unit = {},
    onOpenCategories: () -> Unit = {},
    onAdd: () -> Unit = {},
    viewModel: InsightsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    Column(Modifier.fillMaxSize()) {
        // Outside the scroll, so the title and month switcher stay put.
        Header(state, viewModel::previousMonth, viewModel::nextMonth)
        val scroll = Modifier.verticalScroll(rememberScrollState()).padding(ZenithSpacing.space4)
        state?.takeIf { !it.history.enough }?.let {
            NotEnoughHistory(it.history, it.summary.expense, onAdd, scroll)
        }
        state?.takeIf { it.history.enough }?.let {
            InsightsContent(
                state = it,
                onOpenBudgets = onOpenBudgets,
                onOpenPayees = onOpenPayees,
                onOpenCategories = onOpenCategories,
                modifier = scroll
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Header(state: InsightsState?, onPrevious: () -> Unit, onNext: () -> Unit) {
    FlowRow(
        modifier = Modifier.fillMaxWidth().padding(horizontal = ZenithSpacing.space4, vertical = ZenithSpacing.space2),
        horizontalArrangement = Arrangement.SpaceBetween,
        itemVerticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "Insights",
            style = ZenithType.heading,
            color = zenithColors().ink,
            modifier = Modifier.semantics { heading() }
        )
        if (state != null) {
            MonthSwitcher(
                label = "${state.period.startDate.shortMonthName()} ${state.period.startDate.year}",
                onPrevious = onPrevious,
                onNext = onNext,
                nextEnabled = !state.isCurrentMonth
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun InsightsContent(
    state: InsightsState,
    onOpenBudgets: () -> Unit,
    onOpenPayees: () -> Unit,
    onOpenCategories: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(ZenithSpacing.space2)) {
        CashFlowCard(state)
        BudgetHealthCard(state, onOpenBudgets)
        if (state.history.trendReady(state.today)) TrendCard(state)
        WhereItWentCard(state)
        SharedMoneyCard(state.shared, state.today, onOpenPayees)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(ZenithSpacing.space2)) {
            SecondaryButton("Budgets", onOpenBudgets, Modifier.weight(1f))
            SecondaryButton("Payees", onOpenPayees, Modifier.weight(1f))
            SecondaryButton("Categories", onOpenCategories, Modifier.weight(1f))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CashFlowCard(state: InsightsState) {
    val colors = zenithColors()
    val summary = state.summary
    val start = state.period.startDate
    val end = if (state.isCurrentMonth) state.today else state.period.endDate.minusDays(1)
    val range = "${start.dayOfMonth} ${start.shortMonthName()} to ${end.dayOfMonth} ${end.shortMonthName()}"
    ZenithCard {
        Text("Net cash flow · $range", style = ZenithType.caption, color = colors.inkMuted)
        TotalAmount(
            summary.net,
            signed = true,
            tone = when {
                summary.net > 0 -> AmountTone.Positive
                summary.net < 0 -> AmountTone.Negative
                else -> AmountTone.Neutral
            }
        )
        state.keptPercent?.let { kept ->
            val words = if (kept >= 0) "Kept $kept% of income" else "Spent ${-kept}% more than income"
            Text(words, style = ZenithType.caption, color = colors.inkMuted)
            ProgressTrack(
                progress = summary.expense.toFloat() / summary.income,
                description = words
            )
        }
        FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text("Income", style = ZenithType.caption, color = colors.inkMuted)
                AmountText(summary.income, signed = true, tone = AmountTone.Positive)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("Personal expenses", style = ZenithType.caption, color = colors.inkMuted)
                AmountText(-summary.expense)
            }
        }
    }
}

@Composable
private fun BudgetHealthCard(state: InsightsState, onOpenBudgets: () -> Unit) {
    val colors = zenithColors()
    val budgets = state.budgets
    val worst = worstBudgets(budgets.categories)
    val onTrack = budgets.categories.count { it.status == BudgetStatus.ON_TRACK }
    ZenithCard {
        SectionHeader("Budget health", actionLabel = "All budgets", onAction = onOpenBudgets)
        if (budgets.categories.isEmpty()) {
            Text("No budgets for this month.", style = ZenithType.body, color = colors.inkMuted)
        } else {
            Text(
                "The tick marks where an even pace would be (${(budgets.pace * 100).toInt()}%).",
                style = ZenithType.caption,
                color = colors.inkMuted
            )
            worst.forEach { line ->
                BudgetLineRow(state.names.categories[line.categoryId] ?: "Uncategorized", line, budgets.pace)
            }
            if (onTrack > 0) {
                val more = if (worst.isEmpty()) "" else "more "
                Text("$onTrack ${more}on track", style = ZenithType.body, color = colors.ink)
            }
        }
    }
}

@Composable
private fun TrendCard(state: InsightsState) {
    val colors = zenithColors()
    val trend = state.trend
    val spent = trend.months.last().expense
    // A past month is already complete, so there is nothing to project.
    val projection = trend.projection.takeIf { state.isCurrentMonth }
    val figure = projection ?: spent
    val difference = figure - trend.fiveMonthAverage
    ZenithCard {
        SectionHeader("Spending trend")
        Text(
            if (projection != null) "On pace for ~${MoneyFormatter.format(projection).text} this month"
            else "${MoneyFormatter.format(spent).text} in ${state.period.startDate.monthName()}",
            style = ZenithType.bodyStrong,
            color = colors.ink
        )
        Text(
            "${MoneyFormatter.format(abs(difference)).text} ${if (difference >= 0) "above" else "below"} " +
                "your 5-month average (dashed line)",
            style = ZenithType.caption,
            color = colors.inkMuted
        )
        MonthlyBars(
            bars = trend.months.map { MonthBar(it.period.startDate.shortMonthName(), it.expense) },
            average = trend.fiveMonthAverage,
            projection = projection
        )
    }
}

@Composable
private fun WhereItWentCard(state: InsightsState) {
    val colors = zenithColors()
    ZenithCard {
        SectionHeader("Where it went")
        if (state.categories.isEmpty()) {
            Text("Nothing spent in this month.", style = ZenithType.body, color = colors.inkMuted)
        } else {
            ShareBar(state.categories.map { (name, amount) -> ShareSlice(name, amount) })
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SharedMoneyCard(shared: SharedBalanceSummary, today: LocalDate, onOpenPayees: () -> Unit) {
    val colors = zenithColors()
    ZenithCard {
        SectionHeader("Shared money", actionLabel = "Open", onAction = onOpenPayees)
        FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(ZenithSpacing.space2)) {
            Column(Modifier.weight(1f)) {
                Text("Owed to you", style = ZenithType.caption, color = colors.inkMuted)
                AmountText(shared.owedToYou, signed = true, tone = AmountTone.Positive, style = ZenithType.heading)
            }
            Column(Modifier.weight(1f)) {
                Text("You owe", style = ZenithType.caption, color = colors.inkMuted)
                AmountText(-shared.youOwe, signed = true, tone = AmountTone.Negative, style = ZenithType.heading)
            }
        }
        if (shared.oldestFirst.isNotEmpty()) {
            Text("Oldest first", style = ZenithType.caption, color = colors.inkMuted)
        }
        shared.oldestFirst.take(OLDEST_SHOWN).forEach { line -> OpenBalanceRow(line, today) }
    }
}

@Composable
private fun OpenBalanceRow(line: PayeeBalanceLine, today: LocalDate) {
    val since = line.oldestOpenAt?.toLocalDate()
    ListRow(
        title = if (line.net > 0) "${line.name} owes you" else "You owe ${line.name}",
        subtitle = since?.let {
            val days = ChronoUnit.DAYS.between(it, today)
            "$days ${if (days == 1L) "day" else "days"} · since ${dayLabel(it, today)}"
        },
        leading = { Avatar(line.name, Modifier.clearAndSetSemantics {}) },
        trailing = {
            AmountText(
                line.net,
                signed = true,
                tone = if (line.net > 0) AmountTone.Positive else AmountTone.Negative
            )
        }
    )
}

@Preview
@Composable
private fun InsightsPreview() {
    val today = LocalDate.of(2026, 10, 20)
    val period = MonthPeriods.containing(today)
    val months = generateSequence(period) { MonthPeriods.previous(it) }.take(6).toList().reversed()
    val state = InsightsState(
        today = today,
        period = period,
        isCurrentMonth = true,
        summary = MonthSummary(income = 3_200_000, expense = 1_845_000),
        budgets = MonthBudgets(
            period, null, listOf(BudgetLine(1, 600_000, 684_000, BudgetStatus.OVER)), 0.65f, null, null, true
        ),
        trend = MonthTrend(
            months.mapIndexed { i, m -> MonthTotal(m, 2_200_000L + i * 100_000) }, 2_530_000, 2_860_000
        ),
        categories = listOf("Food" to 684_000L, "Transport" to 274_000L),
        shared = SharedBalanceSummary(
            685_000, 212_000, 473_000, 4, 2, listOf(PayeeBalanceLine(1, "Kabir", 320_000, 0))
        ),
        names = TxnNames(categories = mapOf(1L to "Food")),
        history = History(40, today.minusMonths(6))
    )
    PalettePreview { InsightsContent(state, {}, {}, {}) }
}
