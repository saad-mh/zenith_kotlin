package com.saadm.zenith.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.saadm.zenith.ui.theme.ZenithSpacing
import com.saadm.zenith.ui.theme.ZenithType
import com.saadm.zenith.ui.theme.zenithColors

enum class StatusTone { Positive, Negative, Caution, Neutral }

/** Status as an icon and a word. Colour is never the only signal. */
@Composable
fun StatusTag(text: String, tone: StatusTone, modifier: Modifier = Modifier) {
    val colors = zenithColors()
    val tint = when (tone) {
        StatusTone.Positive -> colors.positive
        StatusTone.Negative -> colors.negative
        StatusTone.Caution -> colors.caution
        StatusTone.Neutral -> colors.inkMuted
    }
    val icon: ImageVector? = when (tone) {
        StatusTone.Positive -> Icons.Rounded.CheckCircle
        StatusTone.Negative -> Icons.Rounded.ErrorOutline
        StatusTone.Caution -> Icons.Rounded.Schedule
        StatusTone.Neutral -> null
    }
    val shape = MaterialTheme.shapes.small
    Row(
        modifier = modifier
            .clip(shape)
            .background(colors.surfaceSunken, shape)
            .padding(horizontal = ZenithSpacing.space2, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(ZenithSpacing.space1))
        }
        Text(text = text, style = ZenithType.caption, color = tint)
    }
}
