package com.saadm.zenith.ui.add

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.saadm.zenith.ui.components.Lu
import com.saadm.zenith.ui.theme.ThemeMode
import com.saadm.zenith.ui.theme.ZenithSpacing
import com.saadm.zenith.ui.theme.ZenithTheme
import com.saadm.zenith.ui.theme.ZenithType
import com.saadm.zenith.ui.theme.zenithColors

/** Digits, backspace, + and −, with the save key bottom right. */
@Composable
fun NumPad(onKey: (String) -> Unit, saveLabel: String, canSave: Boolean, onSave: () -> Unit) {
    val rows = listOf(
        listOf("1", "2", "3", BACKSPACE),
        listOf("4", "5", "6", PLUS),
        listOf("7", "8", "9", MINUS_KEY)
    )
    val spacing = Arrangement.spacedBy(ZenithSpacing.space2)
    Column(Modifier.fillMaxWidth(), verticalArrangement = spacing) {
        rows.forEach { keys ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = spacing) {
                keys.forEach { key -> PadKey(key, { onKey(key) }, Modifier.weight(1f)) }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = spacing) {
            PadKey(".", { onKey(".") }, Modifier.weight(1f))
            PadKey("0", { onKey("0") }, Modifier.weight(1f))
            SaveKey(saveLabel, canSave, onSave, Modifier.weight(2f))
        }
    }
}

private val keyHeight = 60.dp

@Composable
private fun PadKey(key: String, onClick: () -> Unit, modifier: Modifier) {
    val colors = zenithColors()
    val shape = MaterialTheme.shapes.medium
    val description = when (key) {
        BACKSPACE -> "Backspace"
        "." -> "Decimal point"
        PLUS -> "Plus"
        MINUS_KEY -> "Minus"
        else -> key
    }
    val operator = key == BACKSPACE || key == PLUS || key == MINUS_KEY
    Box(
        modifier = modifier
            .defaultMinSize(minHeight = keyHeight)
            .clip(shape)
            .background(if (operator) colors.surfaceSunken else colors.surfaceRaised, shape)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center
    ) {
        if (key == BACKSPACE) {
            Icon(painterResource(Lu.lucide_ic_delete), contentDescription = null, tint = colors.ink)
        } else {
            Text(text = if (key == MINUS_KEY) "−" else key, style = ZenithType.heading, color = colors.ink)
        }
    }
}

@Composable
private fun SaveKey(label: String, enabled: Boolean, onClick: () -> Unit, modifier: Modifier) {
    val colors = zenithColors()
    val shape = MaterialTheme.shapes.medium
    Box(
        modifier = modifier
            .defaultMinSize(minHeight = keyHeight)
            .clip(shape)
            .background(if (enabled) colors.ink else colors.surfaceSunken, shape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painterResource(Lu.lucide_ic_check),
            contentDescription = null,
            tint = if (enabled) colors.surface else colors.inkMuted,
            modifier = Modifier.size(28.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewNumPad() {
    ZenithTheme(mode = ThemeMode.Dark) {
        NumPad(onKey = {}, saveLabel = "Save", canSave = true, onSave = {})
    }
}
