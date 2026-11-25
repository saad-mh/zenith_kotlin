package com.saadm.zenith.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.saadm.zenith.ui.theme.ZenithSpacing
import com.saadm.zenith.ui.theme.zenithColors

/** Raised surface with a 1dp border and the large (12dp) radius. */
@Composable
fun ZenithCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val colors = zenithColors()
    val shape = MaterialTheme.shapes.large
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.surfaceRaised, shape)
            .border(1.dp, colors.border, shape)
            .padding(ZenithSpacing.space4),
        content = content
    )
}
