package com.saadm.zenith.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.saadm.zenith.ui.components.Lu
import com.saadm.zenith.ui.components.ZenithCard
import com.saadm.zenith.ui.theme.ZenithSpacing
import com.saadm.zenith.ui.theme.ZenithType
import com.saadm.zenith.ui.theme.zenithColors

/** Fill of a settings icon tile. The icon on it is white, so each is dark enough to carry it. */
enum class Tile(val color: Color) {
    Blue(Color(0xFF2563A8)),
    Orange(Color(0xFFB4532A)),
    Plum(Color(0xFF8E4585)),
    Indigo(Color(0xFF5A5FB0)),
    Green(Color(0xFF1F7A4F)),
    Teal(Color(0xFF0F7C8C)),
    Gold(Color(0xFF84652B)),
    Red(Color(0xFFB8433A))
}

/** A captioned group of rows on one tile. */
@Composable
fun SettingsGroup(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(ZenithSpacing.space1)) {
        Text(
            title.uppercase(),
            style = ZenithType.caption,
            color = zenithColors().inkMuted,
            modifier = Modifier.padding(start = ZenithSpacing.space1, top = ZenithSpacing.space3).semantics { heading() }
        )
        ZenithCard(content = content)
    }
}

@Composable
private fun IconTile(icon: Int, tile: Tile) {
    val colors = zenithColors()
    Box(
        modifier = Modifier
            .size(32.dp)
            .background(if (colors.isMono) colors.ink else tile.color, MaterialTheme.shapes.extraSmall),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painterResource(icon),
            contentDescription = null,
            tint = if (colors.isMono) colors.surface else Color.White,
            modifier = Modifier.size(18.dp)
        )
    }
}

private fun Modifier.rowFrame(): Modifier = fillMaxWidth().defaultMinSize(minHeight = 52.dp).padding(vertical = 6.dp)

/** Icon, label, current value and a chevron. Without [onClick] it only shows the value. */
@Composable
fun SettingsRow(icon: Int, tile: Tile, label: String, value: String? = null, onClick: (() -> Unit)? = null) {
    val colors = zenithColors()
    // At large font sizes the value goes under the label instead of squeezing it.
    val large = LocalDensity.current.fontScale >= 1.5f
    Row(
        modifier = (if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier.semantics(mergeDescendants = true) {})
            .rowFrame(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(ZenithSpacing.space3)
    ) {
        IconTile(icon, tile)
        Column(Modifier.weight(1f)) {
            Text(label, style = ZenithType.body, color = colors.ink)
            if (large && value != null) Text(value, style = ZenithType.caption, color = colors.inkMuted)
        }
        if (!large && value != null) Text(value, style = ZenithType.body, color = colors.inkMuted)
        if (onClick != null) {
            Icon(painterResource(Lu.lucide_ic_chevron_right), contentDescription = null, tint = colors.inkMuted, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
fun SettingsSwitchRow(icon: Int, tile: Tile, label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange).rowFrame(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(ZenithSpacing.space3)
    ) {
        IconTile(icon, tile)
        Text(label, style = ZenithType.body, color = zenithColors().ink, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = null)
    }
}
