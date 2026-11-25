package com.saadm.zenith.ui.components

import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.PieChart
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.saadm.zenith.ui.theme.ZenithTheme
import com.saadm.zenith.ui.theme.ZenithType
import com.saadm.zenith.ui.theme.zenithColors

data class PillTab(val route: String, val label: String, val icon: ImageVector)

data class PrimaryActionSpec(
    val icon: ImageVector,
    val contentDescription: String,
    val onClick: () -> Unit
)

/**
 * Floating pill: 64dp tall with three tabs and a 48dp circular action.
 * The selected tab expands to icon and label on a tinted pill. The others show an icon, with a
 * content description, and are at least 48dp tall.
 */
@Composable
fun NavPillBar(
    tabs: List<PillTab>,
    selectedRoute: String?,
    onTabSelected: (String) -> Unit,
    primaryAction: PrimaryActionSpec,
    modifier: Modifier = Modifier
) {
    val colors = zenithColors()
    val pill = RoundedCornerShape(percent = 50)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .height(64.dp)
            .shadow(elevation = 8.dp, shape = pill)
            .clip(pill)
            .background(colors.surfaceRaised, pill)
            .border(1.dp, colors.border, pill)
            .padding(horizontal = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        tabs.forEach { tab ->
            val selected = tab.route == selectedRoute
            PillTabItem(
                tab = tab,
                selected = selected,
                onClick = { onTabSelected(tab.route) },
                modifier = if (selected) Modifier else Modifier.weight(1f)
            )
        }
        Spacer(Modifier.width(2.dp))
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(colors.primaryStrong, CircleShape)
                .clickable(role = Role.Button, onClick = primaryAction.onClick)
                .semantics { contentDescription = primaryAction.contentDescription },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = primaryAction.icon,
                contentDescription = null,
                tint = colors.onStrong,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun PillTabItem(
    tab: PillTab,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier
) {
    val colors = zenithColors()
    val pill = RoundedCornerShape(percent = 50)
    Row(
        modifier = modifier
            .height(48.dp)
            .clip(pill)
            .then(if (selected) Modifier.background(colors.avatarTints[0].bg, pill) else Modifier)
            .selectable(selected = selected, role = Role.Tab, onClick = onClick)
            .padding(horizontal = if (selected) 16.dp else 0.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        if (selected) {
            Icon(tab.icon, contentDescription = null, tint = colors.link, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            val scale = LocalDensity.current.fontScale
            val size = ZenithType.bodyStrong.fontSize * (minOf(scale, 1.3f) / scale)
            Text(text = tab.label, style = ZenithType.bodyStrong.copy(fontSize = size), color = colors.link, maxLines = 1)
        } else {
            Icon(tab.icon, contentDescription = tab.label, tint = colors.inkMuted, modifier = Modifier.size(20.dp))
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun NavPillBarPreview() {
    ZenithTheme {
        NavPillBar(
            tabs = listOf(
                PillTab("home", "Overview", Icons.Rounded.Home),
                PillTab("insights", "Insights", Icons.Rounded.PieChart),
                PillTab("settings", "Settings", Icons.Rounded.Settings),
            ),
            selectedRoute = "home",
            onTabSelected = {},
            primaryAction = PrimaryActionSpec(Icons.Rounded.Add, "Add transaction", {})
        )
    }
}
