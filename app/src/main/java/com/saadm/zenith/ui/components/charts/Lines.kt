package com.saadm.zenith.ui.components.charts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.saadm.zenith.domain.model.MoneyFormatter
import com.saadm.zenith.ui.theme.zenithColors

/**
 * Running total for this month (solid c1, end marker) against last month (dashed c3).
 * Both series share one scale.
 */
@Composable
fun CumulativeLine(
    current: List<Long>,
    modifier: Modifier = Modifier,
    previous: List<Long>? = null
) {
    val description = buildString {
        append("Running total this month. ")
        current.lastOrNull()?.let { append("Now ${MoneyFormatter.format(it).spoken}. ") }
        previous?.lastOrNull()?.let { append("Last month at the same point ${MoneyFormatter.format(it).spoken}.") }
    }
    LineSeriesCanvas(
        series = listOfNotNull(current, previous),
        dashedSeries = if (previous != null) 1 else null,
        modifier = modifier.height(160.dp).semantics { contentDescription = description }
    )
}

@Composable
private fun LineSeriesCanvas(
    series: List<List<Long>>,
    dashedSeries: Int?,
    modifier: Modifier
) {
    val colors = zenithColors()
    val top = series.flatten().maxOrNull()?.coerceAtLeast(1L) ?: 1L
    // One x scale for every series, so a month that is only part way through stops part way across.
    val points = series.maxOfOrNull { it.size } ?: 0
    Canvas(modifier.fillMaxWidth()) {
        val pad = 6.dp.toPx()
        val width = size.width - 2 * pad
        val height = size.height - 2 * pad
        val dash = PathEffect.dashPathEffect(floatArrayOf(8.dp.toPx(), 6.dp.toPx()))
        series.forEachIndexed { seriesIndex, values ->
            if (values.isEmpty()) return@forEachIndexed
            val step = if (points > 1) width / (points - 1) else 0f
            val path = Path()
            values.forEachIndexed { i, value ->
                val point = Offset(pad + step * i, pad + height * (1 - value.toFloat() / top))
                if (i == 0) path.moveTo(point.x, point.y) else path.lineTo(point.x, point.y)
            }
            val isDashed = seriesIndex == dashedSeries
            drawPath(
                path = path,
                color = if (isDashed) colors.chart(2) else colors.chart(0),
                style = Stroke(
                    width = (if (isDashed) 1.5f else 2f).dp.toPx(),
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round,
                    pathEffect = if (isDashed) dash else null
                )
            )
            if (!isDashed) {
                val last = values.last()
                drawCircle(
                    color = colors.chart(0),
                    radius = 4.dp.toPx(),
                    center = Offset(pad + step * (values.size - 1), pad + height * (1 - last.toFloat() / top))
                )
            }
        }
    }
}
