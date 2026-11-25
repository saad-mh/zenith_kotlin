package com.saadm.zenith.ui.components

import com.saadm.zenith.ui.theme.ZenithColors
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.saadm.zenith.ui.theme.ZenithSpacing
import com.saadm.zenith.ui.theme.ZenithType
import com.saadm.zenith.ui.theme.zenithColors

enum class StatusTone { Positive, Negative, Caution, Neutral }

/** Chip fill and text for a tone: green, yellow and red. Monochrome stays grey. */
fun statusChipColors(tone: StatusTone, colors: ZenithColors): Pair<Color, Color> = when {
    colors.isMono || tone == StatusTone.Neutral -> colors.surfaceSunken to colors.ink
    tone == StatusTone.Positive ->
        if (colors.isDark) Color(0xFF1E4632) to Color(0xFFA8E8C6) else Color(0xFFD7F0E2) to Color(0xFF145C39)
    tone == StatusTone.Caution ->
        if (colors.isDark) Color(0xFF4D4012) to Color(0xFFFFE38A) else Color(0xFFFFE99A) to Color(0xFF594300)
    else ->
        if (colors.isDark) Color(0xFF57231F) to Color(0xFFFFC2BC) else Color(0xFFFBDAD7) to Color(0xFF8F241C)
}

/** Status as an icon and words. Colour is never the only signal. */
@Composable
fun StatusTag(text: String, tone: StatusTone, modifier: Modifier = Modifier) {
    val colors = zenithColors()
    val (fill, tint) = statusChipColors(tone, colors)
    val icon: Int? = when (tone) {
        StatusTone.Positive -> Lu.lucide_ic_circle_check
        StatusTone.Negative -> Lu.lucide_ic_circle_alert
        StatusTone.Caution -> Lu.lucide_ic_clock
        StatusTone.Neutral -> null
    }
    val shape = MaterialTheme.shapes.extraSmall
    Row(
        modifier = modifier
            .clip(shape)
            .background(fill, shape)
            .padding(horizontal = ZenithSpacing.space2, vertical = ZenithSpacing.space1),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(painterResource(icon), contentDescription = null, tint = tint, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(ZenithSpacing.space1))
        }
        Text(text = text, style = ZenithType.caption, color = tint)
    }
}
