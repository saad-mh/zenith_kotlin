package com.saadm.zenith.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.saadm.zenith.domain.budget.BudgetLine
import com.saadm.zenith.domain.budget.BudgetStatus
import com.saadm.zenith.domain.model.MoneyFormatter
import com.saadm.zenith.ui.theme.ZenithSpacing
import com.saadm.zenith.ui.theme.ZenithType
import com.saadm.zenith.ui.theme.zenithColors
import kotlin.math.abs

/** One budget in the grid. "left" or "over", the chip's icon and its line carry the status; colour only repeats it. */
@Composable
fun BudgetTile(
    name: String,
    icon: Int?,
    line: BudgetLine,
    pace: Float?,
    daysLeft: Long?,
    quip: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val colors = zenithColors()
    val ratio = if (line.limit > 0) line.spent.toFloat() / line.limit else 0f
    val shape = MaterialTheme.shapes.large
    Column(
        modifier = modifier
            .clip(shape)
            .background(colors.surfaceRaised, shape)
            .then(
                if (onClick != null) Modifier.clickable(onClickLabel = "Edit budget", role = Role.Button, onClick = onClick)
                else Modifier.semantics(mergeDescendants = true) {}
            )
            .padding(ZenithSpacing.space3),
        verticalArrangement = Arrangement.spacedBy(ZenithSpacing.space1)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(ZenithSpacing.space2)) {
            if (icon != null) {
                Icon(painterResource(icon), contentDescription = null, tint = colors.ink, modifier = Modifier.size(16.dp))
            }
            Text(name, style = ZenithType.bodyStrong, color = colors.ink)
        }
        if (daysLeft != null) Text("$daysLeft days left", style = ZenithType.caption, color = colors.inkMuted)
        Spacer(Modifier.size(ZenithSpacing.space2))
        Text("${(ratio * 100).toInt()}% spent", style = ZenithType.caption, color = colors.inkMuted)
        AmountText(abs(line.left), style = ZenithType.heading)
        Text(if (line.status == BudgetStatus.OVER) "over" else "left", style = ZenithType.caption, color = colors.inkMuted)
        StatusTag(
            quip,
            when (line.status) {
                BudgetStatus.OVER -> StatusTone.Negative
                BudgetStatus.NEARING -> StatusTone.Caution
                BudgetStatus.ON_TRACK -> StatusTone.Positive
            }
        )
        Spacer(Modifier.weight(1f))
        ProgressTrack(
            progress = ratio,
            description = "${MoneyFormatter.format(line.spent).spoken} of ${MoneyFormatter.format(line.limit).spoken}" +
                (pace?.let { ", even pace is ${(it * 100).toInt()} percent" } ?: ""),
            paceTick = pace,
            fillColor = when (line.status) {
                BudgetStatus.OVER -> colors.negative
                BudgetStatus.NEARING -> colors.caution
                BudgetStatus.ON_TRACK -> colors.primaryStrong
            }
        )
    }
}
