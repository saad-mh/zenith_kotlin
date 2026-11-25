package com.saadm.zenith.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.saadm.zenith.ui.theme.ZenithSpacing
import com.saadm.zenith.ui.theme.ZenithType
import com.saadm.zenith.ui.theme.zenithColors

/** Filled brand button. 48dp minimum touch target. */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    destructive: Boolean = false
) {
    val colors = zenithColors()
    ZenithButton(
        text = text,
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        minHeight = 52.dp,
        container = when {
            !enabled -> colors.surfaceSunken
            destructive -> colors.negative
            else -> colors.primaryStrong
        },
        border = null,
        textColor = when {
            !enabled -> colors.inkMuted
            destructive -> colors.surface
            else -> colors.onStrong
        }
    )
}

/** Outlined button on a raised surface. 44dp minimum touch target. */
@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val colors = zenithColors()
    ZenithButton(
        text = text,
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        minHeight = 48.dp,
        container = colors.surfaceSunken,
        border = null,
        textColor = if (enabled) colors.ink else colors.inkMuted
    )
}

/** Text-only action, such as a trailing "See all" link. 44dp minimum touch target. */
@Composable
fun ZenithTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    ZenithButton(
        text = text,
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        minHeight = 44.dp,
        container = Color.Transparent,
        border = null,
        textColor = zenithColors().link
    )
}

@Composable
private fun ZenithButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier,
    enabled: Boolean,
    minHeight: Dp,
    container: Color,
    border: Color?,
    textColor: Color
) {
    val shape = MaterialTheme.shapes.medium
    Box(
        modifier = modifier
            .defaultMinSize(minHeight = minHeight)
            .clip(shape)
            .background(container, shape)
            .then(if (border != null) Modifier.border(1.dp, border, shape) else Modifier)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = ZenithSpacing.space4, vertical = ZenithSpacing.space2),
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, style = ZenithType.bodyStrong, color = textColor)
    }
}
