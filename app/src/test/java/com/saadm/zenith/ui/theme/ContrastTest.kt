package com.saadm.zenith.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.saadm.zenith.ui.components.StatusTone
import com.saadm.zenith.ui.components.statusChipColors
import org.junit.Assert.assertEquals
import org.junit.Test

/** WCAG AA (4.5:1) for every text colour on every surface it is drawn on, in all four palettes. */
class ContrastTest {

    private fun ratio(a: Color, b: Color): Float {
        val (high, low) = listOf(a.luminance(), b.luminance()).sortedDescending()
        return (high + 0.05f) / (low + 0.05f)
    }

    @Test
    fun textColoursMeetAa() {
        val palettes = mapOf(
            "light" to ZenithLightColors,
            "dark" to ZenithDarkColors,
            "mono" to ZenithMonochromeColors,
            "mono dark" to ZenithMonochromeDarkColors,
        )
        val failures = palettes.flatMap { (name, c) ->
            val text = mapOf(
                "ink" to c.ink, "muted" to c.inkMuted, "positive" to c.positive, "negative" to c.negative,
                "caution" to c.caution, "link" to c.link, "gold" to c.goldAccent,
            )
            val surfaces = mapOf("surface" to c.surface, "raised" to c.surfaceRaised, "sunken" to c.surfaceSunken)
            val pairs = text.flatMap { (fg, f) -> surfaces.map { (bg, b) -> Triple("$fg on $bg", f, b) } } +
                c.avatarTints.mapIndexed { i, t -> Triple("tint $i", t.fg, t.bg) } +
                Triple("button text", c.onStrong, c.primaryStrong) +
                Triple("selected tab", c.link, c.avatarTints[0].bg) +
                StatusTone.entries.map { tone ->
                    val (fill, text) = statusChipColors(tone, c)
                    Triple("chip $tone", text, fill)
                }
            pairs.filter { ratio(it.second, it.third) < 4.5f }
                .map { "$name: ${it.first} ${ratio(it.second, it.third)}" }
        }
        assertEquals(emptyList<String>(), failures)
    }

    // Bars, lines and share segments are graphics: WCAG asks 3:1 against what they sit on.
    @Test
    fun chartColoursStandOutFromTheirBackground() {
        val failures = listOf(ZenithLightColors, ZenithDarkColors, ZenithMonochromeColors, ZenithMonochromeDarkColors)
            .flatMapIndexed { palette, c ->
                c.chartColors.flatMapIndexed { i, colour ->
                    listOf(c.surface, c.surfaceRaised).filter { ratio(colour, it) < 3f }
                        .map { "palette $palette c${i + 1} ${ratio(colour, it)}" }
                }
            }
        assertEquals(emptyList<String>(), failures)
    }
}
