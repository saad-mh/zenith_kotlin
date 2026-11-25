package com.saadm.zenith.ui.components.charts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.saadm.zenith.domain.model.MoneyFormatter
import com.saadm.zenith.ui.theme.ZenithSpacing
import com.saadm.zenith.ui.theme.ZenithType
import com.saadm.zenith.ui.theme.zenithColors
import kotlin.math.cos
import kotlin.math.sin

/**
 * Semicircle showing [spent] against [limit], with a pace tick at [paceFraction]
 * (the share of the period that has passed). Over the limit the arc is full and the label says so.
 */
@Composable
fun Gauge(
    spent: Long,
    limit: Long,
    paceFraction: Float,
    modifier: Modifier = Modifier
) {
    val colors = zenithColors()
    val ratio = if (limit > 0) spent.toFloat() / limit else 0f
    val fill = ratio.coerceIn(0f, 1f)
    val over = spent > limit
    val percentText = "${(ratio * 100).toInt()}%"
    val description = "Spent ${MoneyFormatter.format(spent).spoken} of ${MoneyFormatter.format(limit).spoken}, " +
        "${(ratio * 100).toInt()} percent. Pace tick at ${(paceFraction * 100).toInt()} percent of the period." +
        if (over) " Over budget." else ""

    Box(
        modifier = modifier.width(200.dp).semantics { contentDescription = description },
        contentAlignment = Alignment.BottomCenter
    ) {
        Canvas(Modifier.size(width = 200.dp, height = 110.dp)) {
            val stroke = 14.dp.toPx()
            val inset = stroke / 2
            val arcSize = Size(size.width - stroke, (size.width - stroke))
            drawArc(
                color = colors.surfaceSunken,
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
            if (fill > 0f) {
                drawArc(
                    color = if (over) colors.negative else colors.chart(0),
                    startAngle = 180f,
                    sweepAngle = 180f * fill,
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                )
            }
            val radius = arcSize.width / 2
            val centre = Offset(size.width / 2, inset + radius)
            val angle = Math.toRadians((180.0 + 180.0 * paceFraction.coerceIn(0f, 1f)))
            val inner = radius - stroke
            val outer = radius + stroke / 2
            drawLine(
                color = colors.ink,
                start = Offset(
                    centre.x + inner * cos(angle).toFloat(),
                    centre.y + inner * sin(angle).toFloat()
                ),
                end = Offset(
                    centre.x + outer * cos(angle).toFloat(),
                    centre.y + outer * sin(angle).toFloat()
                ),
                strokeWidth = 2.dp.toPx()
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = percentText, style = ZenithType.heading, color = colors.ink)
            Text(
                text = if (over) "Over budget" else "of budget",
                style = ZenithType.caption,
                color = colors.inkMuted
            )
        }
    }
}

/**
 * One ring per budget. A ring caps at full; the label carries the overspend
 * ("+₹1,200 over") so the ring alone never says it.
 */
@Composable
fun BudgetRings(rings: List<BudgetRing>, modifier: Modifier = Modifier) {
    val colors = zenithColors()
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(ZenithSpacing.space4)
    ) {
        rings.forEach { ring ->
            val over = ring.spent > ring.limit
            val fill = if (ring.limit > 0) (ring.spent.toFloat() / ring.limit).coerceIn(0f, 1f) else 0f
            val label = if (over) {
                "+${MoneyFormatter.format(ring.spent - ring.limit).text} over"
            } else {
                MoneyFormatter.format(ring.limit - ring.spent).text + " left"
            }
            val description = "${ring.label}: spent ${MoneyFormatter.format(ring.spent).spoken} of " +
                "${MoneyFormatter.format(ring.limit).spoken}. " + if (over) "Over budget." else "Within budget."
            Column(
                modifier = Modifier.semantics { contentDescription = description },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(ZenithSpacing.space1)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(64.dp)) {
                    Canvas(Modifier.size(64.dp)) {
                        val stroke = 8.dp.toPx()
                        val inset = stroke / 2
                        val ringSize = Size(size.width - stroke, size.height - stroke)
                        drawArc(
                            color = colors.surfaceSunken,
                            startAngle = -90f,
                            sweepAngle = 360f,
                            useCenter = false,
                            topLeft = Offset(inset, inset),
                            size = ringSize,
                            style = Stroke(width = stroke)
                        )
                        drawArc(
                            color = if (over) colors.negative else colors.chart(0),
                            startAngle = -90f,
                            sweepAngle = 360f * fill,
                            useCenter = false,
                            topLeft = Offset(inset, inset),
                            size = ringSize,
                            style = Stroke(width = stroke)
                        )
                    }
                    Text(
                        text = "${(ring.spent * 100 / ring.limit.coerceAtLeast(1)).coerceAtMost(999)}%",
                        style = ZenithType.caption,
                        color = colors.ink
                    )
                }
                Text(
                    text = ring.label,
                    style = ZenithType.caption,
                    color = colors.ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.width(80.dp)
                )
                Text(
                    text = label,
                    style = ZenithType.caption,
                    color = if (over) colors.negative else colors.inkMuted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.width(80.dp)
                )
            }
        }
    }
}
