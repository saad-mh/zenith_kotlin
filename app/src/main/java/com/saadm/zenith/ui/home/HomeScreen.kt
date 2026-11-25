package com.saadm.zenith.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.saadm.zenith.data.entity.TransactionEntity
import com.saadm.zenith.data.entity.TxnType
import com.saadm.zenith.domain.account.NetPosition
import com.saadm.zenith.domain.model.MoneyFormatter
import com.saadm.zenith.domain.model.MonthPeriods
import com.saadm.zenith.domain.payee.PayeeBalanceLine
import com.saadm.zenith.domain.payee.PayeeReminder
import com.saadm.zenith.domain.payee.SharedBalanceSummary
import com.saadm.zenith.domain.transaction.TxnItems
import com.saadm.zenith.domain.transaction.TxnNames
import com.saadm.zenith.domain.transaction.dayLabel
import com.saadm.zenith.ui.common.PalettePreview
import com.saadm.zenith.ui.common.monthName
import com.saadm.zenith.ui.common.shareText
import com.saadm.zenith.ui.common.shortMonthName
import com.saadm.zenith.ui.components.AmountText
import com.saadm.zenith.ui.components.AmountTone
import com.saadm.zenith.ui.components.Avatar
import com.saadm.zenith.ui.components.ListRow
import com.saadm.zenith.ui.components.SecondaryButton
import com.saadm.zenith.ui.components.SectionHeader
import com.saadm.zenith.ui.components.TxnItemRow
import com.saadm.zenith.ui.components.ZenithCard
import com.saadm.zenith.ui.components.charts.CumulativeLine
import com.saadm.zenith.ui.theme.ZenithSpacing
import com.saadm.zenith.ui.theme.ZenithType
import com.saadm.zenith.ui.theme.zenithColors
import java.time.LocalDate
import kotlin.math.abs

@Composable
fun HomeScreen(
    onOpenSettings: () -> Unit = {},
    onOpenTransactions: () -> Unit = {},
    onOpenPayees: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    OverviewContent(
        state = state,
        onOpenSettings = onOpenSettings,
        onOpenTransactions = onOpenTransactions,
        onOpenPayees = onOpenPayees,
        onRemind = { line -> PayeeReminder.textFor(line.name, line.net)?.let(context::shareText) },
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(ZenithSpacing.space4)
    )
}

@Composable
private fun OverviewContent(
    state: OverviewState?,
    onOpenSettings: () -> Unit,
    onOpenTransactions: () -> Unit,
    onOpenPayees: () -> Unit,
    onRemind: (PayeeBalanceLine) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = zenithColors()
    Column(modifier, verticalArrangement = Arrangement.spacedBy(ZenithSpacing.space2)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Zenith",
                style = ZenithType.heading,
                color = colors.ink,
                modifier = Modifier.weight(1f).semantics { heading() }
            )
            IconButton(onClick = onOpenSettings) {
                val name = state?.profileName.orEmpty()
                if (name.isBlank()) {
                    Icon(Icons.Rounded.Person, contentDescription = "Profile and settings", tint = colors.ink)
                } else {
                    Avatar(name, Modifier.semantics { contentDescription = "Profile and settings" })
                }
            }
        }
        if (state == null) return@Column

        ZenithCard {
            Text("Net position", style = ZenithType.caption, color = colors.inkMuted)
            AmountText(state.netPosition.net, style = ZenithType.display)
            Text(state.netPosition.subtitle(), style = ZenithType.caption, color = colors.inkMuted)
        }
        SpendCard(state.spend)
        FriendsCard(state.shared, state.nudge, onOpenPayees, onRemind)

        SectionHeader("Recent", actionLabel = "See all", onAction = onOpenTransactions)
        ZenithCard {
            if (state.recent.isEmpty()) {
                Text("No entries yet. Tap + to add your first one.", style = ZenithType.body, color = colors.inkMuted)
            }
            state.recent.forEach { recent ->
                TxnItemRow(recent.item, caption = dayLabel(recent.date, state.today))
            }
        }
    }
}

@Composable
private fun SpendCard(spend: MonthSpend) {
    val colors = zenithColors()
    val thisMonth = spend.period.startDate
    val lastMonth = thisMonth.minusMonths(1).shortMonthName()
    val comparison = if (spend.difference == 0L) {
        "Same as $lastMonth at this point"
    } else {
        val word = if (spend.difference < 0) "less" else "more"
        "${MoneyFormatter.format(abs(spend.difference)).text} $word than $lastMonth at this point"
    }
    ZenithCard {
        Text("Spent in ${thisMonth.monthName()}", style = ZenithType.caption, color = colors.inkMuted)
        AmountText(spend.spent, style = ZenithType.heading)
        Text(comparison, style = ZenithType.caption, color = colors.inkMuted)
        CumulativeLine(
            current = spend.thisSoFar,
            previous = spend.lastMonth,
            modifier = Modifier.padding(top = ZenithSpacing.space2).height(64.dp)
        )
        Text("Solid: this month · Dashed: $lastMonth", style = ZenithType.caption, color = colors.inkMuted)
    }
}

@Composable
private fun FriendsCard(
    shared: SharedBalanceSummary,
    nudge: Nudge?,
    onOpenPayees: () -> Unit,
    onRemind: (PayeeBalanceLine) -> Unit
) {
    val colors = zenithColors()
    ZenithCard {
        SectionHeader("Between friends", actionLabel = "Open", onAction = onOpenPayees)
        Row(Modifier.fillMaxWidth()) {
            Column(Modifier.weight(1f)) {
                Text("Owed to you · ${people(shared.peopleOwingYou)}", style = ZenithType.caption, color = colors.inkMuted)
                AmountText(shared.owedToYou, signed = true, tone = AmountTone.Positive, style = ZenithType.heading)
            }
            Column(Modifier.weight(1f)) {
                Text("You owe · ${people(shared.peopleYouOwe)}", style = ZenithType.caption, color = colors.inkMuted)
                AmountText(-shared.youOwe, signed = true, tone = AmountTone.Negative, style = ZenithType.heading)
            }
        }
        Row {
            Text("Net ", style = ZenithType.caption, color = colors.inkMuted)
            AmountText(shared.net, signed = true, style = ZenithType.caption)
        }
        if (nudge != null) {
            HorizontalDivider(Modifier.padding(top = ZenithSpacing.space2), color = colors.border)
            ListRow(
                title = "${nudge.line.name} hasn't paid ${MoneyFormatter.format(nudge.line.net).text}",
                subtitle = when (nudge.daysOpen) {
                    0L -> "Pending since today"
                    1L -> "1 day pending"
                    else -> "${nudge.daysOpen} days pending"
                },
                trailing = { SecondaryButton("Remind", onClick = { onRemind(nudge.line) }) }
            )
        }
    }
}

private fun people(count: Int): String = if (count == 1) "1 person" else "$count people"

@Preview
@Composable
private fun OverviewPreview() {
    val today = LocalDate.of(2026, 10, 20)
    val coffee = TransactionEntity(
        id = 1, amount = 42_000, type = TxnType.EXPENSE, transactedAt = 0, createdAt = 0, updatedAt = 0,
        categoryId = 1, payeeId = null, accountId = null, note = "Third Wave Coffee", receiptUri = null
    )
    val dev = PayeeBalanceLine(1, "Dev", 32_000, null)
    val state = OverviewState(
        today = today,
        netPosition = NetPosition(10_234_000, 4, 473_000),
        spend = MonthSpend(
            MonthPeriods.containing(today), 1_845_000, 2_060_000,
            thisSoFar = listOf(100, 300, 700, 900), lastMonth = listOf(200, 500, 600, 1100, 1400, 1500)
        ),
        shared = SharedBalanceSummary(685_000, 212_000, 473_000, 4, 2, listOf(dev)),
        nudge = Nudge(dev, 2),
        recent = TxnItems.of(listOf(coffee), TxnNames(mapOf(1L to "Food"))).map { RecentTxn(it, today) }
    )
    PalettePreview { OverviewContent(state, {}, {}, {}, {}) }
}
