package com.saadm.zenith.ui.components.charts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.saadm.zenith.domain.model.MoneyFormatter
import com.saadm.zenith.ui.components.AmountText
import com.saadm.zenith.ui.theme.ZenithSpacing
import com.saadm.zenith.ui.theme.ZenithType
import com.saadm.zenith.ui.theme.zenithColors

/**
 * One bar per month. The current month is drawn in c1 and the others in c3, so the highlight
 * survives monochrome. [projection] outlines the expected full month; [average] is a dashed line.
 */
@Composable
fun MonthlyBars(
    bars: List<MonthBar>,
    modifier: Modifier = Modifier,
    currentIndex: Int? = bars.lastIndex,
    average: Long? = null,
    projection: Long? = null
) {
    val colors = zenithColors()
    val textMeasurer = rememberTextMeasurer()
    val labelStyle = ZenithType.caption.copy(color = colors.inkMuted)
    val top = maxOf(bars.maxOfOrNull { it.amount } ?: 0L, projection ?: 0L, average ?: 0L).coerceAtLeast(1L)
    val description = buildString {
        append("Monthly spending. ")
        bars.forEachIndexed { index, bar ->
            append("${bar.label} ${MoneyFormatter.format(bar.amount).spoken}")
            if (index == currentIndex) append(", this month")
            append(". ")
        }
        average?.let { append("Average ${MoneyFormatter.format(it).spoken}. ") }
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(180.dp)
            .semantics { contentDescription = description }
    ) {
        val labelHeight = textMeasurer.measure("A", labelStyle).size.height + 4.dp.toPx()
        val plotTop = 8.dp.toPx()
        val plotBottom = size.height - labelHeight
        val plotHeight = plotBottom - plotTop
        val slot = size.width / bars.size.coerceAtLeast(1)
        val barWidth = slot * 0.56f
        val dash = PathEffect.dashPathEffect(floatArrayOf(8.dp.toPx(), 6.dp.toPx()))
        fun yFor(value: Long): Float = plotBottom - plotHeight * (value.toFloat() / top)

        bars.forEachIndexed { index, bar ->
            val x = slot * index + (slot - barWidth) / 2
            val isCurrent = index == currentIndex
            val color = if (isCurrent) colors.chart(0) else colors.chart(2)
            val barTop = yFor(bar.amount)
            drawRoundRect(
                color = color,
                topLeft = Offset(x, barTop),
                size = Size(barWidth, plotBottom - barTop),
                cornerRadius = CornerRadius(4.dp.toPx())
            )
            if (isCurrent && projection != null && projection > bar.amount) {
                val projectedTop = yFor(projection)
                drawRoundRect(
                    color = color,
                    topLeft = Offset(x, projectedTop),
                    size = Size(barWidth, barTop - projectedTop),
                    cornerRadius = CornerRadius(4.dp.toPx()),
                    style = Stroke(width = 1.5.dp.toPx(), pathEffect = dash)
                )
            }
            val label = textMeasurer.measure(bar.label, labelStyle)
            drawText(label, topLeft = Offset(x + (barWidth - label.size.width) / 2, plotBottom + 4.dp.toPx()))
        }

        average?.let {
            val y = yFor(it)
            drawLine(
                color = colors.ink,
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 1.5.dp.toPx(),
                pathEffect = dash
            )
        }
    }
}

/** Horizontal bars in the order given (sort them first). Each row shows the label and the amount. */
@Composable
fun RankedBars(items: List<RankedItem>, modifier: Modifier = Modifier) {
    val colors = zenithColors()
    val top = (items.maxOfOrNull { it.amount } ?: 0L).coerceAtLeast(1L)
    val description = "Ranked. " + items.joinToString(". ") { "${it.label} ${MoneyFormatter.format(it.amount).spoken}" }
    Column(
        modifier = modifier.fillMaxWidth().semantics { contentDescription = description },
        verticalArrangement = Arrangement.spacedBy(ZenithSpacing.space2)
    ) {
        items.forEachIndexed { index, item ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(ZenithSpacing.space2)
            ) {
                Text(
                    text = item.label,
                    style = ZenithType.body,
                    color = colors.ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.width(96.dp)
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(10.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(colors.surfaceSunken)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(item.amount.toFloat() / top)
                            .fillMaxHeight()
                            .background(colors.chart(index))
                    )
                }
                AmountText(amount = item.amount, style = ZenithType.numeric)
            }
        }
    }
}

/** One column per period, stacked by segment (at most four). Segments use c1..c4 bottom to top. */
@Composable
fun StackedBars(
    columns: List<StackedColumn>,
    segmentLabels: List<String>,
    modifier: Modifier = Modifier
) {
    require(columns.all { it.segments.size <= 4 }) { "StackedBars takes at most four segments per column" }
    val colors = zenithColors()
    val textMeasurer = rememberTextMeasurer()
    val labelStyle = ZenithType.caption.copy(color = colors.inkMuted)
    val top = (columns.maxOfOrNull { it.segments.sum() } ?: 0L).coerceAtLeast(1L)
    val description = "Stacked by ${segmentLabels.joinToString(", ")}. " +
        columns.joinToString(". ") { column ->
            "${column.label} ${MoneyFormatter.format(column.segments.sum()).spoken}"
        }

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(ZenithSpacing.space2)) {
        Canvas(Modifier.fillMaxWidth().height(180.dp).semantics { contentDescription = description }) {
            val labelHeight = textMeasurer.measure("A", labelStyle).size.height + 4.dp.toPx()
            val plotBottom = size.height - labelHeight
            val plotHeight = plotBottom - 8.dp.toPx()
            val slot = size.width / columns.size.coerceAtLeast(1)
            val barWidth = slot * 0.56f
            columns.forEachIndexed { index, column ->
                val x = slot * index + (slot - barWidth) / 2
                var bottom = plotBottom
                column.segments.forEachIndexed { segment, amount ->
                    val height = plotHeight * (amount.toFloat() / top)
                    if (height > 0f) {
                        drawRect(
                            color = colors.chart(segment),
                            topLeft = Offset(x, bottom - height),
                            size = Size(barWidth, height)
                        )
                        bottom -= height
                    }
                }
                val label = textMeasurer.measure(column.label, labelStyle)
                drawText(label, topLeft = Offset(x + (barWidth - label.size.width) / 2, plotBottom + 4.dp.toPx()))
            }
        }
        SeriesLegend(segmentLabels)
    }
}

/** Two series side by side per period, c1 and c2. */
@Composable
fun GroupedBars(
    groups: List<GroupedBar>,
    firstLabel: String,
    secondLabel: String,
    modifier: Modifier = Modifier
) {
    val colors = zenithColors()
    val textMeasurer = rememberTextMeasurer()
    val labelStyle = ZenithType.caption.copy(color = colors.inkMuted)
    val top = groups.maxOfOrNull { maxOf(it.first, it.second) }?.coerceAtLeast(1L) ?: 1L
    val description = "Grouped by month, $firstLabel and $secondLabel. " +
        groups.joinToString(". ") {
            "${it.label}: $firstLabel ${MoneyFormatter.format(it.first).spoken}, " +
                "$secondLabel ${MoneyFormatter.format(it.second).spoken}"
        }

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(ZenithSpacing.space2)) {
        Canvas(Modifier.fillMaxWidth().height(180.dp).semantics { contentDescription = description }) {
            val labelHeight = textMeasurer.measure("A", labelStyle).size.height + 4.dp.toPx()
            val plotBottom = size.height - labelHeight
            val plotHeight = plotBottom - 8.dp.toPx()
            val slot = size.width / groups.size.coerceAtLeast(1)
            val pairWidth = slot * 0.6f
            val barWidth = pairWidth / 2 - 2.dp.toPx()
            groups.forEachIndexed { index, group ->
                val left = slot * index + (slot - pairWidth) / 2
                listOf(group.first to colors.chart(0), group.second to colors.chart(1))
                    .forEachIndexed { series, (amount, color) ->
                        val height = plotHeight * (amount.toFloat() / top)
                        drawRect(
                            color = color,
                            topLeft = Offset(left + series * (barWidth + 4.dp.toPx()), plotBottom - height),
                            size = Size(barWidth, height)
                        )
                    }
                val label = textMeasurer.measure(group.label, labelStyle)
                drawText(label, topLeft = Offset(slot * index + (slot - label.size.width) / 2, plotBottom + 4.dp.toPx()))
            }
        }
        SeriesLegend(listOf(firstLabel, secondLabel))
    }
}

/**
 * Gains up from a zero line in c1, losses down in c2. The sign is always printed
 * on the bar, so the colour is never the only signal.
 */
@Composable
fun NetFlowBars(bars: List<NetFlowBar>, modifier: Modifier = Modifier) {
    val colors = zenithColors()
    val textMeasurer = rememberTextMeasurer()
    val labelStyle = ZenithType.caption.copy(color = colors.inkMuted)
    val valueStyle = ZenithType.caption.copy(color = colors.ink)
    val extreme = (bars.maxOfOrNull { kotlin.math.abs(it.net) } ?: 0L).coerceAtLeast(1L)
    val description = "Net cash flow. " + bars.joinToString(". ") {
        "${it.label} ${MoneyFormatter.format(it.net, signed = true).spoken}"
    }

    Canvas(modifier.fillMaxWidth().height(200.dp).semantics { contentDescription = description }) {
        val labelHeight = textMeasurer.measure("A", labelStyle).size.height + 4.dp.toPx()
        val plotTop = 16.dp.toPx()
        val plotBottom = size.height - labelHeight
        val zeroY = (plotTop + plotBottom) / 2
        val halfHeight = (plotBottom - plotTop) / 2
        val slot = size.width / bars.size.coerceAtLeast(1)
        val barWidth = slot * 0.56f
        drawLine(
            color = colors.border,
            start = Offset(0f, zeroY),
            end = Offset(size.width, zeroY),
            strokeWidth = 1.dp.toPx()
        )
        bars.forEachIndexed { index, bar ->
            val x = slot * index + (slot - barWidth) / 2
            val height = halfHeight * (kotlin.math.abs(bar.net).toFloat() / extreme)
            val gain = bar.net >= 0
            val barTop = if (gain) zeroY - height else zeroY
            drawRect(
                color = colors.chart(if (gain) 0 else 1),
                topLeft = Offset(x, barTop),
                size = Size(barWidth, height)
            )
            val value = textMeasurer.measure(MoneyFormatter.format(bar.net, signed = true).text, valueStyle)
            val valueY = if (gain) barTop - value.size.height - 2.dp.toPx() else barTop + height + 2.dp.toPx()
            drawText(value, topLeft = Offset(x + (barWidth - value.size.width) / 2, valueY))
            val label = textMeasurer.measure(bar.label, labelStyle)
            drawText(label, topLeft = Offset(x + (barWidth - label.size.width) / 2, plotBottom + 4.dp.toPx()))
        }
    }
}

/** Legend for a series, one swatch per name, in c1..cN order. */
@Composable
internal fun SeriesLegend(names: List<String>, modifier: Modifier = Modifier) {
    val colors = zenithColors()
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(ZenithSpacing.space3)
    ) {
        names.forEachIndexed { index, name ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(ZenithSpacing.space1)) {
                Box(
                    Modifier
                        .size(10.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(colors.chart(index))
                )
                Text(text = name, style = ZenithType.caption, color = colors.inkMuted)
            }
        }
    }
}
