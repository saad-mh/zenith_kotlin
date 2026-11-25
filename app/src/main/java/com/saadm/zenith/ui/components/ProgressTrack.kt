package com.saadm.zenith.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.saadm.zenith.ui.theme.zenithColors

/**
 * Bar showing [progress] (0 to 1; values above 1 fill the bar).
 * [paceTick] draws an even-pace marker at that position, for budgets.
 */
@Composable
fun ProgressTrack(
    progress: Float,
    description: String,
    modifier: Modifier = Modifier,
    paceTick: Float? = null,
    fillColor: Color? = null
) {
    val colors = zenithColors()
    val fraction = progress.coerceIn(0f, 1f)
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(12.dp)
            .semantics {
                contentDescription = description
                progressBarRangeInfo = ProgressBarRangeInfo(fraction, 0f..1f)
            }
    ) {
        val barHeight = 6.dp.toPx()
        val top = (size.height - barHeight) / 2
        val radius = CornerRadius(barHeight / 2)
        drawRoundRect(
            color = colors.surfaceSunken,
            topLeft = Offset(0f, top),
            size = Size(size.width, barHeight),
            cornerRadius = radius
        )
        if (fraction > 0f) {
            drawRoundRect(
                color = fillColor ?: colors.primaryStrong,
                topLeft = Offset(0f, top),
                size = Size(size.width * fraction, barHeight),
                cornerRadius = radius
            )
        }
        paceTick?.let { tick ->
            val x = size.width * tick.coerceIn(0f, 1f)
            // A halo in the page colour keeps the tick visible over a filled bar.
            drawLine(color = colors.surface, start = Offset(x, 0f), end = Offset(x, size.height), strokeWidth = 6.dp.toPx())
            drawLine(
                color = colors.ink,
                start = Offset(x, 0f),
                end = Offset(x, size.height),
                strokeWidth = 2.dp.toPx()
            )
        }
    }
}
