package com.saadm.zenith.ui.components.charts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import com.saadm.zenith.domain.model.MoneyFormatter
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
                color = colors.surfaceSunken,
                topLeft = Offset(x, plotTop),
                size = Size(barWidth, plotHeight),
                cornerRadius = CornerRadius(6.dp.toPx())
            )
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

