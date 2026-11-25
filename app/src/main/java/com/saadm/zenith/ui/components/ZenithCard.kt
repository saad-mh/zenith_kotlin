package com.saadm.zenith.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import com.saadm.zenith.ui.theme.ZenithSpacing
import com.saadm.zenith.ui.theme.zenithColors

/** A flat tile: soft fill, no border, large radius. */
@Composable
fun ZenithCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val shape = MaterialTheme.shapes.large
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(zenithColors().surfaceRaised, shape)
            .padding(ZenithSpacing.space4),
        content = content
    )
}
