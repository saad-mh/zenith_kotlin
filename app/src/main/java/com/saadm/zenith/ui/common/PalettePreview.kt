package com.saadm.zenith.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.saadm.zenith.ui.theme.ThemeMode
import com.saadm.zenith.ui.theme.ZenithPalette
import com.saadm.zenith.ui.theme.ZenithSpacing
import com.saadm.zenith.ui.theme.ZenithTheme
import com.saadm.zenith.ui.theme.zenithColors

/** Draws [content] in light, dark and monochrome, one under the other, for screen previews. */
@Composable
fun PalettePreview(content: @Composable () -> Unit) {
    Column {
        listOf(
            ThemeMode.Light to ZenithPalette.Colour,
            ThemeMode.Dark to ZenithPalette.Colour,
            ThemeMode.Light to ZenithPalette.Monochrome
        ).forEach { (mode, palette) ->
            ZenithTheme(mode = mode, palette = palette) {
                Column(Modifier.background(zenithColors().surface).padding(ZenithSpacing.space4)) { content() }
            }
        }
    }
}
