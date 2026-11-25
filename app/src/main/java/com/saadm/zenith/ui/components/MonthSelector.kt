package com.saadm.zenith.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.saadm.zenith.ui.theme.ZenithSpacing
import com.saadm.zenith.ui.theme.ZenithType
import com.saadm.zenith.ui.theme.zenithColors

/**
 * Pill with previous and next month buttons around [label], e.g. "Oct 2026".
 * Pass [nextEnabled] = false on the current month.
 */
@Composable
fun MonthSwitcher(
    label: String,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
    nextEnabled: Boolean = true
) {
    val colors = zenithColors()
    val pill = RoundedCornerShape(percent = 50)
    Row(
        modifier = modifier
            .clip(pill)
            .background(colors.surfaceRaised, pill)
            .border(1.dp, colors.border, pill)
            .padding(horizontal = ZenithSpacing.space1),
        verticalAlignment = Alignment.CenterVertically
    ) {
        StepButton(icon = Icons.Rounded.ChevronLeft, description = "Previous month", onClick = onPrevious)
        Text(
            text = label,
            style = ZenithType.bodyStrong,
            color = colors.ink,
            modifier = Modifier.padding(horizontal = ZenithSpacing.space2)
        )
        StepButton(
            icon = Icons.Rounded.ChevronRight,
            description = "Next month",
            enabled = nextEnabled,
            onClick = onNext
        )
    }
}

@Composable
private fun StepButton(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    val colors = zenithColors()
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (enabled) colors.ink else colors.inkMuted
        )
    }
}
