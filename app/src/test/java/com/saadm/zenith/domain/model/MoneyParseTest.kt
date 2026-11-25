package com.saadm.zenith.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MoneyParseTest {

    @Test
    fun readsRupeesIntoPaise() {
        assertEquals(65_000L, MoneyFormatter.parse("650"))
        assertEquals(65_050L, MoneyFormatter.parse("650.5"))
        assertEquals(10_707_000L, MoneyFormatter.parse(" 1,07,070 "))
        assertEquals(5L, MoneyFormatter.parse(".05"))
    }

    @Test
    fun refusesAnythingElse() {
        listOf("", "0", "-5", "1.234", "abc", "1e3", "999999999999999999").forEach {
            assertNull(it, MoneyFormatter.parse(it))
        }
    }

    @Test
    fun plainRoundTrips() {
        assertEquals("650", MoneyFormatter.plain(65_000))
        assertEquals("650.05", MoneyFormatter.plain(65_005))
    }
}
