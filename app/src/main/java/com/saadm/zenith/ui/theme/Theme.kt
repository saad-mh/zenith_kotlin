package com.saadm.zenith.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.platform.LocalContext

enum class ThemeMode { System, Light, Dark }

val LocalZenithColors = compositionLocalOf { ZenithLightColors }

/** The brand tokens of the current theme. */
@Composable
@ReadOnlyComposable
fun zenithColors(): ZenithColors = LocalZenithColors.current

/**
 * Brand palette by default. [dynamicColor] (Android 12+) swaps only the Material 3 scheme
 * for the wallpaper-derived one; [zenithColors] keeps the brand tokens.
 */
@Composable
fun ZenithTheme(
    mode: ThemeMode = ThemeMode.System,
    palette: ZenithPalette = ZenithPalette.Colour,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val dark = when (mode) {
        ThemeMode.System -> isSystemInDarkTheme()
        ThemeMode.Light -> false
        ThemeMode.Dark -> true
    }
    val brand = zenithColorsFor(palette, dark)
    val scheme = if (dynamicColor) {
        val context = LocalContext.current
        if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else {
        brand.toColorScheme()
    }

    CompositionLocalProvider(LocalZenithColors provides brand) {
        MaterialTheme(
            colorScheme = scheme,
            typography = Typography,
            content = content
        )
    }
}

// Maps brand tokens onto Material 3 roles so stock components pick up the palette.
private fun ZenithColors.toColorScheme(): ColorScheme {
    val base = if (isDark) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = primaryStrong,
        onPrimary = onStrong,
        primaryContainer = avatarTints[0].bg,
        onPrimaryContainer = link,
        secondary = inkMuted,
        onSecondary = surface,
        tertiary = goldAccent,
        onTertiary = surface,
        background = surface,
        onBackground = ink,
        surface = surface,
        onSurface = ink,
        surfaceVariant = surfaceSunken,
        onSurfaceVariant = inkMuted,
        surfaceContainerLowest = surface,
        surfaceContainerLow = surfaceSunken,
        surfaceContainer = surfaceRaised,
        surfaceContainerHigh = surfaceRaised,
        surfaceContainerHighest = surfaceRaised,
        outline = border,
        outlineVariant = border,
        error = negative,
        onError = surface,
        scrim = scrim,
    )
}
