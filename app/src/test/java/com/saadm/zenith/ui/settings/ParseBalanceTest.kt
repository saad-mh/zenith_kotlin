package com.saadm.zenith.ui.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ParseBalanceTest {

    @Test
    fun readsOpeningBalances() {
        assertEquals(0L, parseBalance(""))
        assertEquals(0L, parseBalance("0"))
        assertEquals(150_000L, parseBalance("1,500"))
        assertEquals(-25_050L, parseBalance("-250.50"))
        assertNull(parseBalance("-"))
        assertNull(parseBalance("12a"))
    }
}
