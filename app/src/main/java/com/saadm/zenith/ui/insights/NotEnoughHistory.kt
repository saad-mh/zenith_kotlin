package com.saadm.zenith.ui.insights

import com.saadm.zenith.ui.components.Lu
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.saadm.zenith.ui.common.PalettePreview
import com.saadm.zenith.ui.components.AmountText
import com.saadm.zenith.ui.components.EmptyState
import com.saadm.zenith.ui.components.ListRow
import com.saadm.zenith.ui.components.ZenithCard
import com.saadm.zenith.ui.theme.ZenithSpacing
import com.saadm.zenith.ui.theme.ZenithType
import com.saadm.zenith.ui.theme.zenithColors
import java.time.LocalDate

private const val ENTRIES_NEEDED = 5
private const val TREND_DAYS = 14L

/** How much has been recorded: income and expense entries, and the day of the first one. */
data class History(val entries: Int, val firstEntry: LocalDate?) {
    val enough: Boolean get() = entries >= ENTRIES_NEEDED
    val toGo: Int get() = (ENTRIES_NEEDED - entries).coerceAtLeast(0)

    /** A trend over a few days would mislead, so it waits for two weeks. */
    fun trendReady(today: LocalDate): Boolean =
        firstEntry != null && !firstEntry.isAfter(today.minusDays(TREND_DAYS - 1))
}

/** What Insights shows before there is enough to compare: the figure so far and what unlocks next. */
@Composable
fun NotEnoughHistory(history: History, spent: Long, onAdd: () -> Unit, modifier: Modifier = Modifier) {
    val colors = zenithColors()
    Column(modifier, verticalArrangement = Arrangement.spacedBy(ZenithSpacing.space3)) {
        EmptyState(
            title = "Not enough history yet",
            body = "Trends and comparisons need a few more entries before they say anything true.",
            actionLabel = "Add a transaction",
            onAction = onAdd,
            example = {
                ZenithCard(Modifier.fillMaxWidth()) {
                    Text("Spent so far this month", style = ZenithType.caption, color = colors.inkMuted)
                    AmountText(spent, style = ZenithType.display)
                    Text(
                        if (history.entries == 1) "1 entry" else "${history.entries} entries",
                        style = ZenithType.caption,
                        color = colors.inkMuted
                    )
                }
            }
        )
        ZenithCard {
            Text("Unlocks as you add entries", style = ZenithType.subheading, color = colors.ink)
            listOf(
                "Category breakdown" to "Needs $ENTRIES_NEEDED entries · ${history.toGo} to go",
                "Spending trend" to "Needs 2 weeks of entries",
                "Compared with last month" to "Needs one full month"
            ).forEach { (title, needs) ->
                ListRow(
                    title = title,
                    subtitle = needs,
                    leading = { Icon(painterResource(Lu.lucide_ic_lock), contentDescription = "Locked", tint = colors.inkMuted) }
                )
            }
        }
    }
}

@Preview
@Composable
private fun NotEnoughHistoryPreview() {
    PalettePreview { NotEnoughHistory(History(3, LocalDate.of(2026, 10, 18)), 125_000, {}) }
}
