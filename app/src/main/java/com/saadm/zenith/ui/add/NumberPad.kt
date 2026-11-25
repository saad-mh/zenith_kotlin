package com.saadm.zenith.ui.add

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.saadm.zenith.ui.theme.ThemeMode
import com.saadm.zenith.ui.theme.ZenithSpacing
import com.saadm.zenith.ui.theme.ZenithTheme
import com.saadm.zenith.ui.theme.ZenithType
import com.saadm.zenith.ui.theme.zenithColors

@Composable
fun NumPad(onKey: (String) -> Unit) {
    val rows = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf(".", "0", BACKSPACE)
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(ZenithSpacing.space1)
    ) {
        rows.forEach { rowKeys ->
            Row(modifier = Modifier.fillMaxWidth()) {
                rowKeys.forEach { key -> PadKey(key = key, onClick = { onKey(key) }, modifier = Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun PadKey(key: String, onClick: () -> Unit, modifier: Modifier) {
    val colors = zenithColors()
    val shape = MaterialTheme.shapes.medium
    val description = when (key) {
        BACKSPACE -> "Backspace"
        "." -> "Decimal point"
        else -> key
    }
    Box(
        modifier = modifier
            .padding(ZenithSpacing.space1)
            .defaultMinSize(minHeight = 56.dp)
            .clip(shape)
            .background(colors.surfaceSunken, shape)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center
    ) {
        if (key == BACKSPACE) {
            Icon(Icons.AutoMirrored.Rounded.Backspace, contentDescription = null, tint = colors.ink)
        } else {
            Text(text = key, style = ZenithType.heading, color = colors.ink)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewNumPad() {
    ZenithTheme(mode = ThemeMode.Dark) {
        NumPad(onKey = {})
    }
}
