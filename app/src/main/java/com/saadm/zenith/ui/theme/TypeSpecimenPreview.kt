package com.saadm.zenith.ui.theme

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview

@Preview(name = "Type specimen", widthDp = 390, heightDp = 760, showBackground = true)
@Composable
private fun TypeSpecimenPreview() {
    ZenithTheme {
        val c = zenithColors()
        Column(
            Modifier.fillMaxSize().padding(ZenithSpacing.space4),
            verticalArrangement = Arrangement.spacedBy(ZenithSpacing.space3),
        ) {
            Text("Display 40/46", style = ZenithType.display, color = c.ink)
            Text("Heading 24/30", style = ZenithType.heading, color = c.ink)
            Text("Subheading 18/26", style = ZenithType.subheading, color = c.ink)
            Text("Body 15/22 for paragraphs and form labels.", style = ZenithType.body, color = c.ink)
            Text("Body strong 15/22", style = ZenithType.bodyStrong, color = c.ink)
            Text("Caption 12/16 for timestamps and helpers", style = ZenithType.caption, color = c.inkMuted)
            Text(
                "Tabular figures: the amounts below should line up on the right.",
                style = ZenithType.caption,
                color = c.inkMuted,
            )
            listOf("1,234.50", "10,070", "123,456", "420").forEach { amount ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Transaction", style = ZenithType.body, color = c.ink)
                    Text(amount, style = ZenithType.numeric, color = c.ink)
                }
            }
        }
    }
}
