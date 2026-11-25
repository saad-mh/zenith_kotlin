package com.saadm.zenith.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Preview(name = "All palettes", widthDp = 1000, heightDp = 640, showBackground = true)
@Composable
private fun PaletteSwatchSheet() {
    Row(Modifier.fillMaxSize()) {
        PaletteColumn("Light", ZenithLightColors, Modifier.weight(1f))
        PaletteColumn("Dark", ZenithDarkColors, Modifier.weight(1f))
        PaletteColumn("Monochrome", ZenithMonochromeColors, Modifier.weight(1f))
        PaletteColumn("Monochrome dark", ZenithMonochromeDarkColors, Modifier.weight(1f))
    }
}

@Composable
private fun PaletteColumn(name: String, c: ZenithColors, modifier: Modifier) {
    Column(
        modifier = modifier.fillMaxHeight().background(c.surface).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(name, color = c.ink, fontSize = 16.sp)
        listOf(
            "surface" to c.surface, "surface raised" to c.surfaceRaised,
            "surface sunken" to c.surfaceSunken, "border" to c.border,
            "ink" to c.ink, "ink muted" to c.inkMuted,
            "primary" to c.primary, "primary strong" to c.primaryStrong,
            "focus ring" to c.focusRing, "gold accent" to c.goldAccent,
            "positive" to c.positive, "negative" to c.negative, "caution" to c.caution,
            "link" to c.link, "on strong" to c.onStrong, "scrim" to c.scrim,
        ).forEach { (label, color) -> Swatch(label, color, c) }
        Text("Avatar tints t1-t4", color = c.inkMuted, fontSize = 12.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            c.avatarTints.forEachIndexed { i, tint ->
                Box(
                    Modifier.size(32.dp).background(tint.bg, CircleShape),
                    contentAlignment = Alignment.Center,
                ) { Text("t${i + 1}", color = tint.fg, fontSize = 11.sp) }
            }
        }
        Text("Chart colours c1-c6", color = c.inkMuted, fontSize = 12.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            c.chartColors.forEach { color ->
                Box(Modifier.size(24.dp).background(color, RoundedCornerShape(4.dp)))
            }
        }
    }
}

@Composable
private fun Swatch(label: String, color: Color, c: ZenithColors) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(20.dp)
                .background(color, RoundedCornerShape(4.dp))
                .border(1.dp, c.border, RoundedCornerShape(4.dp))
        )
        Spacer(Modifier.width(8.dp))
        Text(label, color = c.ink, fontSize = 12.sp)
    }
}
