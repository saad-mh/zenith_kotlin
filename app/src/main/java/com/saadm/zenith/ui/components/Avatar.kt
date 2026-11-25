package com.saadm.zenith.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.saadm.zenith.ui.theme.ZenithType
import com.saadm.zenith.ui.theme.zenithColors

enum class AvatarSize(val diameter: Dp, val textStyle: TextStyle) {
    Small(24.dp, ZenithType.caption),
    Medium(40.dp, ZenithType.bodyStrong),
    XLarge(48.dp, ZenithType.bodyStrong),
}

/** Initial in a tinted circle. The tint is picked from the name, so a person keeps the same colour. */
@Composable
fun Avatar(name: String, modifier: Modifier = Modifier, size: AvatarSize = AvatarSize.Medium) {
    val tint = zenithColors().avatarTints[tintIndex(name)]
    Box(
        modifier = modifier
            .size(size.diameter)
            .clip(CircleShape)
            .background(tint.bg, CircleShape)
            .clearAndSetSemantics { contentDescription = name },
        contentAlignment = Alignment.Center
    ) {
        Text(text = initialOf(name), style = size.textStyle, color = tint.fg)
    }
}

/** Overlapping avatars, with a "+N" chip for anyone past [maxVisible]. */
@Composable
fun AvatarStack(
    names: List<String>,
    modifier: Modifier = Modifier,
    size: AvatarSize = AvatarSize.Small,
    maxVisible: Int = 3
) {
    val colors = zenithColors()
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy((-8).dp)) {
        names.take(maxVisible).forEach { name ->
            Avatar(
                name = name,
                size = size,
                modifier = Modifier.border(2.dp, colors.surfaceRaised, CircleShape)
            )
        }
        val hidden = names.size - maxVisible
        if (hidden > 0) {
            Box(
                modifier = Modifier
                    .size(size.diameter)
                    .clip(CircleShape)
                    .background(colors.surfaceSunken, CircleShape)
                    .border(2.dp, colors.surfaceRaised, CircleShape)
                    .clearAndSetSemantics { contentDescription = "$hidden more" },
                contentAlignment = Alignment.Center
            ) {
                Text(text = "+$hidden", style = size.textStyle, color = colors.inkMuted)
            }
        }
    }
}

private fun tintIndex(name: String): Int = Math.floorMod(name.trim().lowercase().hashCode(), 4)

private fun initialOf(name: String): String = name.trim().firstOrNull()?.uppercase() ?: "?"
