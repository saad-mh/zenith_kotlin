package com.saadm.zenith.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class CategoryIconsTest {

    @Test
    fun everySeededValueHasAnIcon() {
        // The values AppDatabase seeds into categories.emoji.
        listOf("TAG", "DINE", "COMMUTE", "BILL", "MORE", "💰", "🎁").forEach { assertNotNull(it, categoryIcon(it)) }
    }

    @Test
    fun keysAreUnique() {
        assertEquals(categoryIcons.size, categoryIcons.map { it.key }.toSet().size)
    }

    @Test
    fun unknownAndMissingKeysFallBack() {
        assertNull(categoryIcon("FROM_THE_FUTURE"))
        assertEquals("C", fallbackGlyph("FROM_THE_FUTURE", "coffee"))
        assertEquals("C", fallbackGlyph("", "coffee"))
        assertEquals("C", fallbackGlyph(null, "coffee"))
        assertEquals("🏷️", fallbackGlyph("🏷️", "coffee"))
        assertEquals("?", fallbackGlyph(null, " "))
    }
}
