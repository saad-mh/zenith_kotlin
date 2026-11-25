package com.saadm.zenith.ui.theme

import androidx.compose.ui.graphics.Color

/** Background and foreground of one avatar tint (slot t1..t4). */
data class AvatarTint(val bg: Color, val fg: Color)

enum class ZenithPalette { Colour, Monochrome }

/**
 * Brand tokens for one palette. Values come from the design canvas (project/zenith.css).
 * [avatarTints] holds t1..t4 and [chartColors] holds c1..c6.
 */
data class ZenithColors(
    val isDark: Boolean,
    val surface: Color,
    val surfaceRaised: Color,
    val surfaceSunken: Color,
    val border: Color,
    val ink: Color,
    val inkMuted: Color,
    val primary: Color,
    val primaryStrong: Color,
    val focusRing: Color,
    val goldAccent: Color,
    val positive: Color,
    val negative: Color,
    val caution: Color,
    val link: Color,
    val onStrong: Color,
    val scrim: Color,
    val avatarTints: List<AvatarTint>,
    val chartColors: List<Color>,
    val isMono: Boolean = false,
)

val ZenithLightColors = ZenithColors(
    isDark = false,
    surface = Color(0xFFFFFFFF),
    surfaceRaised = Color(0xFFF7F6F4),
    surfaceSunken = Color(0xFFEFEDEA),
    border = Color(0xFFE1DCE6),
    ink = Color(0xFF241F2B),
    inkMuted = Color(0xFF6D6877),
    primary = Color(0xFF777FCF),
    primaryStrong = Color(0xFF5A5FB0),
    focusRing = Color(0xFF5A5FB0),
    goldAccent = Color(0xFF84652B),
    positive = Color(0xFF1F7A4F),
    negative = Color(0xFFB8433A),
    caution = Color(0xFF9C5A0A),
    link = Color(0xFF5A5FB0),
    onStrong = Color(0xFFFFFFFF),
    scrim = Color(0x80241F2B),
    avatarTints = listOf(
        AvatarTint(Color(0xFFE7E8F7), Color(0xFF464B9A)),
        AvatarTint(Color(0xFFF1E9D8), Color(0xFF6E5420)),
        AvatarTint(Color(0xFFEBE7EF), Color(0xFF241F2B)),
        AvatarTint(Color(0xFFDCEFE5), Color(0xFF17603D)),
    ),
    chartColors = listOf(
        Color(0xFF5A5FB0), Color(0xFF8A6A2E), Color(0xFF7C82CC),
        Color(0xFF9C5A0A), Color(0xFF1F7A4F), Color(0xFF6D6877),
    ),
)

val ZenithDarkColors = ZenithColors(
    isDark = true,
    surface = Color(0xFF17131C),
    surfaceRaised = Color(0xFF211C28),
    surfaceSunken = Color(0xFF2D2735),
    border = Color(0xFF38303F),
    ink = Color(0xFFF4F1EC),
    inkMuted = Color(0xFFA9A3B4),
    primary = Color(0xFF8A91DC),
    primaryStrong = Color(0xFF5A5FB0),
    focusRing = Color(0xFFA7ACF0),
    goldAccent = Color(0xFFD9B86F),
    positive = Color(0xFF5FC590),
    negative = Color(0xFFF0897F),
    caution = Color(0xFFE5A85A),
    link = Color(0xFFA7ACF0),
    onStrong = Color(0xFFFFFFFF),
    scrim = Color(0xA8060409),
    avatarTints = listOf(
        AvatarTint(Color(0xFF2B2D52), Color(0xFFC3C7F7)),
        AvatarTint(Color(0xFF3A3020), Color(0xFFE8C985)),
        AvatarTint(Color(0xFF2D2736), Color(0xFFECE8F1)),
        AvatarTint(Color(0xFF1F3B2E), Color(0xFF93DCB4)),
    ),
    chartColors = listOf(
        Color(0xFF8A91DC), Color(0xFFD9B86F), Color(0xFF6B71C4),
        Color(0xFFA08950), Color(0xFFCFD2F5), Color(0xFF7C7489),
    ),
)

val ZenithMonochromeColors = ZenithColors(
    isMono = true,
    isDark = false,
    surface = Color(0xFFFFFFFF),
    surfaceRaised = Color(0xFFF5F5F4),
    surfaceSunken = Color(0xFFEBEBEA),
    border = Color(0xFFDADAD9),
    ink = Color(0xFF1B1B1B),
    inkMuted = Color(0xFF636363),
    primary = Color(0xFF4A4A4A),
    primaryStrong = Color(0xFF1F1F1F),
    focusRing = Color(0xFF1F1F1F),
    goldAccent = Color(0xFF4A4A4A),
    positive = Color(0xFF1B1B1B),
    negative = Color(0xFF1B1B1B),
    caution = Color(0xFF1B1B1B),
    link = Color(0xFF1F1F1F),
    onStrong = Color(0xFFFFFFFF),
    scrim = Color(0x80141414),
    avatarTints = listOf(
        AvatarTint(Color(0xFFE3E3E2), Color(0xFF1B1B1B)),
        AvatarTint(Color(0xFFD2D2D1), Color(0xFF1B1B1B)),
        AvatarTint(Color(0xFFECECEB), Color(0xFF1B1B1B)),
        AvatarTint(Color(0xFFC4C4C3), Color(0xFF1B1B1B)),
    ),
    chartColors = listOf(
        Color(0xFF1F1F1F), Color(0xFF4A4A4A), Color(0xFF6E6E6E),
        Color(0xFF3A3A3A), Color(0xFF808080), Color(0xFF5C5C5C),
    ),
)

// Not in the canvas: the design has no dark monochrome. These are neutral greys
// mirroring ZenithMonochromeColors so a monochrome user in dark mode is not left on a light screen.
val ZenithMonochromeDarkColors = ZenithColors(
    isMono = true,
    isDark = true,
    surface = Color(0xFF151515),
    surfaceRaised = Color(0xFF1E1E1E),
    surfaceSunken = Color(0xFF2C2C2C),
    border = Color(0xFF3A3A3A),
    ink = Color(0xFFF2F2F2),
    inkMuted = Color(0xFFA8A8A8),
    primary = Color(0xFFBDBDBD),
    primaryStrong = Color(0xFFECECEC),
    focusRing = Color(0xFFECECEC),
    goldAccent = Color(0xFFBDBDBD),
    positive = Color(0xFFF2F2F2),
    negative = Color(0xFFF2F2F2),
    caution = Color(0xFFF2F2F2),
    link = Color(0xFFECECEC),
    onStrong = Color(0xFF141414),
    scrim = Color(0x99000000),
    avatarTints = listOf(
        AvatarTint(Color(0xFF2A2A2A), Color(0xFFF2F2F2)),
        AvatarTint(Color(0xFF333333), Color(0xFFF2F2F2)),
        AvatarTint(Color(0xFF262626), Color(0xFFF2F2F2)),
        AvatarTint(Color(0xFF3D3D3D), Color(0xFFF2F2F2)),
    ),
    chartColors = listOf(
        Color(0xFFF0F0F0), Color(0xFFB3B3B3), Color(0xFF8A8A8A),
        Color(0xFF767676), Color(0xFFD4D4D4), Color(0xFF9E9E9E),
    ),
)

fun zenithColorsFor(palette: ZenithPalette, dark: Boolean): ZenithColors = when (palette) {
    ZenithPalette.Colour -> if (dark) ZenithDarkColors else ZenithLightColors
    ZenithPalette.Monochrome -> if (dark) ZenithMonochromeDarkColors else ZenithMonochromeColors
}
