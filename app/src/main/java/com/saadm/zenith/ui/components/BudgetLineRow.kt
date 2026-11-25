package com.saadm.zenith.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import com.saadm.zenith.domain.budget.BudgetLine
import com.saadm.zenith.domain.budget.BudgetStatus
import com.saadm.zenith.domain.model.MoneyFormatter
import com.saadm.zenith.ui.theme.ZenithSpacing
import com.saadm.zenith.ui.theme.ZenithType
import com.saadm.zenith.ui.theme.zenithColors

/** The words and tone for a budget's status. The words alone carry the meaning. */
fun BudgetLine.statusText(): String = when (status) {
    BudgetStatus.OVER -> "${MoneyFormatter.format(-left).text} over"
    BudgetStatus.NEARING -> "Nearly there · ${MoneyFormatter.format(left).text} left"
    BudgetStatus.ON_TRACK -> "On track · ${MoneyFormatter.format(left).text} left"
}

private fun BudgetStatus.tone(): StatusTone = when (this) {
    BudgetStatus.OVER -> StatusTone.Negative
    BudgetStatus.NEARING -> StatusTone.Caution
    BudgetStatus.ON_TRACK -> StatusTone.Positive
}

/** One budget: name and percent, a bar with the even-pace tick, then spend against limit and status. */
@Composable
fun BudgetLineRow(
    name: String,
    line: BudgetLine,
    pace: Float,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val colors = zenithColors()
    val ratio = if (line.limit > 0) line.spent.toFloat() / line.limit else 0f
    val fill = when (line.status) {
        BudgetStatus.OVER -> colors.negative
        BudgetStatus.NEARING -> colors.caution
        BudgetStatus.ON_TRACK -> colors.primaryStrong
    }
    val spent = "${MoneyFormatter.format(line.spent).text} of ${MoneyFormatter.format(line.limit).text}"
    Column(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
            .padding(vertical = ZenithSpacing.space2),
        verticalArrangement = Arrangement.spacedBy(ZenithSpacing.space1)
    ) {
        Row {
            Text(name, style = ZenithType.bodyStrong, color = colors.ink, modifier = Modifier.weight(1f))
            Text("${(ratio * 100).toInt()}%", style = ZenithType.numeric, color = colors.ink)
        }
        ProgressTrack(
            progress = ratio,
            description = "$name, $spent, ${line.statusText()}",
            paceTick = pace,
            fillColor = fill
        )
        // Stacked rather than side by side, so large font sizes wrap instead of clipping.
        Text(spent, style = ZenithType.caption, color = colors.inkMuted)
        StatusTag(line.statusText(), line.status.tone())
    }
}
