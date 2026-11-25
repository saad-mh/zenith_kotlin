package com.saadm.zenith.ui.insights

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class HistoryTest {

    private val today = LocalDate.of(2026, 10, 20)

    @Test
    fun fiveEntriesAreEnough() {
        assertFalse(History(4, today).enough)
        assertEquals(1, History(4, today).toGo)
        assertTrue(History(5, today).enough)
    }

    @Test
    fun trendWaitsForTwoWeeks() {
        assertFalse(History(9, null).trendReady(today))
        assertFalse(History(9, today.minusDays(12)).trendReady(today))
        assertTrue(History(9, today.minusDays(13)).trendReady(today))
    }
}
