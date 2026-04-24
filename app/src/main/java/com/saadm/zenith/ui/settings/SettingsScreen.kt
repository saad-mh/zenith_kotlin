package com.saadm.zenith.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SettingsScreen(
    selectedDurationMillis: Int,
    onDurationSelected: (Int) -> Unit,
    selectedStyle: TransitionStyle,
    onStyleSelected: (TransitionStyle) -> Unit
) {
    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Navigation animation",
                style = MaterialTheme.typography.titleMedium
            )

            Text(
                text = "Duration",
                style = MaterialTheme.typography.labelLarge
            )
            TransitionDurationOptions.forEach { duration ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onDurationSelected(duration) }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = selectedDurationMillis == duration,
                        onClick = { onDurationSelected(duration) }
                    )
                    Text(text = "$duration ms")
                }
            }

            Text(
                text = "Style",
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(top = 8.dp)
            )
            TransitionStyle.entries.forEach { style ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onStyleSelected(style) }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = selectedStyle == style,
                        onClick = { onStyleSelected(style) }
                    )
                    Text(text = style.label)
                }
            }
        }
    }
}
