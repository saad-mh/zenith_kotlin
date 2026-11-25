package com.saadm.zenith.ui.components

import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.shape.RoundedCornerShape
import android.view.HapticFeedbackConstants
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.saadm.zenith.ui.theme.ZenithSpacing
import com.saadm.zenith.ui.theme.ZenithType
import com.saadm.zenith.ui.theme.zenithColors

/** On/off chip, 44dp tall. Selection is shown by fill, a tick and the border, not colour alone. */
@Composable
fun FilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: Int? = null
) {
    val colors = zenithColors()
    val shape = RoundedCornerShape(percent = 50)
    val content = if (selected) colors.onStrong else colors.ink
    // performHapticFeedback stays silent when the system's touch feedback is off.
    val view = LocalView.current
    Row(
        modifier = modifier
            .defaultMinSize(minHeight = 44.dp)
            .clip(shape)
            .background(if (selected) colors.primaryStrong else colors.surfaceSunken, shape)
            .selectable(selected = selected, role = Role.Checkbox) {
                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                onClick()
            }
            .padding(horizontal = ZenithSpacing.space3),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(ZenithSpacing.space2)
    ) {
        if (selected) {
            Icon(painterResource(Lu.lucide_ic_check), contentDescription = null, tint = content, modifier = Modifier.size(16.dp))
        } else if (icon != null) {
            Icon(painterResource(icon), contentDescription = null, tint = content, modifier = Modifier.size(16.dp))
        }
        Text(
            text = label,
            style = ZenithType.body.copy(fontWeight = FontWeight.Medium),
            color = content
        )
    }
}

/**
 * Two to four mutually exclusive options. Each segment is at least 44dp tall.
 * At large font sizes the options stack, so no word is broken across lines.
 */
@Composable
fun SegmentedControl(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = zenithColors()
    val stacked = LocalDensity.current.fontScale >= 1.5f
    val outer = if (stacked) MaterialTheme.shapes.large else RoundedCornerShape(percent = 50)
    val segment = outer
    val view = LocalView.current
    val container = modifier
        .fillMaxWidth()
        .clip(outer)
        .background(colors.surfaceSunken, outer)
        .padding(ZenithSpacing.space1)
    val spacing = Arrangement.spacedBy(ZenithSpacing.space1)
    val option: @Composable (Int, Modifier) -> Unit = { index, itemModifier ->
        val isSelected = index == selectedIndex
        Box(
            modifier = itemModifier
                .defaultMinSize(minHeight = 44.dp)
                .clip(segment)
                .background(if (!isSelected) Color.Transparent else if (colors.isDark) colors.border else colors.surface, segment)
                                .selectable(selected = isSelected, role = Role.Tab) {
                    if (!isSelected) view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                    onSelect(index)
                },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = options[index],
                style = ZenithType.body.copy(
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
                ),
                color = if (isSelected) colors.ink else colors.inkMuted
            )
        }
    }
    if (stacked) {
        Column(container, verticalArrangement = spacing) {
            options.indices.forEach { option(it, Modifier.fillMaxWidth()) }
        }
    } else {
        Row(container, horizontalArrangement = spacing) {
            options.indices.forEach { option(it, Modifier.weight(1f)) }
        }
    }
}

/** Label and switch as one 44dp row. TalkBack reads the label and the state together. */
@Composable
fun ZenithSwitch(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 44.dp)
            .toggleable(
                value = checked,
                enabled = enabled,
                role = Role.Switch,
                onValueChange = onCheckedChange
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(ZenithSpacing.space3)
    ) {
        Text(
            text = label,
            style = ZenithType.body,
            color = zenithColors().ink,
            modifier = Modifier.weight(1f)
        )
        Switch(checked = checked, onCheckedChange = null, enabled = enabled)
    }
}
