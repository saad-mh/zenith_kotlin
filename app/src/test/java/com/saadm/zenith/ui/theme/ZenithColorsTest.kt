package com.saadm.zenith.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.max
import kotlin.math.min

class ZenithColorsTest {

    private fun contrast(a: Color, b: Color): Float {
        val hi = max(a.luminance(), b.luminance())
        val lo = min(a.luminance(), b.luminance())
        return (hi + 0.05f) / (lo + 0.05f)
    }

    // Every text/background pair the screens use: (label, text, background).
    private fun textPairs(c: ZenithColors): List<Triple<String, Color, Color>> = buildList {
        listOf("surface" to c.surface, "raised" to c.surfaceRaised, "sunken" to c.surfaceSunken)
            .forEach { (name, bg) ->
                add(Triple("ink on $name", c.ink, bg))
                add(Triple("inkMuted on $name", c.inkMuted, bg))
            }
        add(Triple("link on surface", c.link, c.surface))
        add(Triple("link on raised", c.link, c.surfaceRaised))
        add(Triple("onStrong on primaryStrong", c.onStrong, c.primaryStrong))
        add(Triple("goldAccent on surface", c.goldAccent, c.surface))
        add(Triple("positive on surface", c.positive, c.surface))
        add(Triple("negative on surface", c.negative, c.surface))
        add(Triple("caution on surface", c.caution, c.surface))
        c.avatarTints.forEachIndexed { i, t -> add(Triple("t${i + 1} fg on bg", t.fg, t.bg)) }
    }

    private fun assertReadable(name: String, c: ZenithColors) {
        val failing = textPairs(c)
            .map { (label, fg, bg) -> label to contrast(fg, bg) }
            .filter { (_, ratio) -> ratio < 4.5f }
            .map { (label, ratio) -> "$label=%.2f".format(ratio) }
        assertTrue("$name palette below 4.5:1: $failing", failing.isEmpty())
    }

    @Test
    fun lightPaletteTextMeetsAA() = assertReadable("light", ZenithLightColors)

    @Test
    fun darkPaletteTextMeetsAA() = assertReadable("dark", ZenithDarkColors)

    @Test
    fun monochromePaletteTextMeetsAA() = assertReadable("monochrome", ZenithMonochromeColors)

    @Test
    fun monochromeDarkPaletteTextMeetsAA() = assertReadable("monochrome dark", ZenithMonochromeDarkColors)

    @Test
    fun paletteSelectionMatchesModeAndPalette() {
        assertTrue(zenithColorsFor(ZenithPalette.Colour, dark = false) == ZenithLightColors)
        assertTrue(zenithColorsFor(ZenithPalette.Colour, dark = true) == ZenithDarkColors)
        assertTrue(zenithColorsFor(ZenithPalette.Monochrome, dark = false) == ZenithMonochromeColors)
        assertTrue(zenithColorsFor(ZenithPalette.Monochrome, dark = true) == ZenithMonochromeDarkColors)
    }
}
