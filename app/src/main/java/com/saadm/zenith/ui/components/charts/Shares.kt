package com.saadm.zenith.ui.components.charts

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.saadm.zenith.domain.model.MoneyFormatter
import com.saadm.zenith.ui.components.AmountText
import com.saadm.zenith.ui.theme.ZenithSpacing
import com.saadm.zenith.ui.theme.ZenithType
import com.saadm.zenith.ui.theme.zenithColors

/** Segmented share bar with 2dp gaps, and a legend row per slice with its percentage. */
@Composable
fun ShareBar(slices: List<ShareSlice>, modifier: Modifier = Modifier) {
    val colors = zenithColors()
    val visible = slices.filter { it.amount > 0 }
    val total = visible.sumOf { it.amount }.coerceAtLeast(1L)
    val description = "Share of ${MoneyFormatter.format(total).spoken}. " +
        visible.joinToString(". ") { "${it.label} ${percent(it.amount, total)} percent" }

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(ZenithSpacing.space3)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(28.dp)
                .semantics { contentDescription = description },
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            visible.forEachIndexed { index, slice ->
                Box(
                    Modifier
                        .weight(slice.amount.toFloat())
                        .height(28.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(colors.chart(index))
                )
            }
        }
        visible.forEachIndexed { index, slice ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(ZenithSpacing.space2)
            ) {
                Box(Modifier.size(10.dp).clip(CircleShape).background(colors.chart(index)))
                Text(text = slice.label, style = ZenithType.body, color = colors.ink, modifier = Modifier.weight(1f))
                Text(text = "${percent(slice.amount, total)}%", style = ZenithType.caption, color = colors.inkMuted)
                AmountText(amount = slice.amount, style = ZenithType.numeric)
            }
        }
    }
}

private fun percent(part: Long, total: Long): Int = (part * 100.0 / total).let { kotlin.math.round(it).toInt() }
