package com.saadm.zenith.domain.split

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SplitTest {

    @Test
    fun equalSplitLeavesOddPaiseWithYou() {
        val owed = Split.equal(1000, listOf(1, 2))
        assertEquals(mapOf(1L to 333L, 2L to 333L), owed)
        assertEquals(334L, 1000 - owed.values.sum())
    }

    @Test
    fun customCannotExceedTheTotal() {
        assertEquals(mapOf(1L to 700L), Split.custom(1000, mapOf(1L to 700L)))
        assertNull(Split.custom(1000, mapOf(1L to 700L, 2L to 301L)))
    }

    @Test
    fun itemsSplitByWhoHadThemAndTheRestEqually() {
        val items = listOf(SplitItem(600, setOf(YOU, 1)), SplitItem(101, setOf(1, 2)))
        // 299 left over, shared three ways: 99 each to the others.
        val owed = Split.byItems(1000, listOf(1, 2), items)!!
        assertEquals(300L + 99 + 51, owed[1])
        assertEquals(99L + 50, owed[2])
        assertTrue(owed.values.sum() <= 1000)
    }

    @Test
    fun itemsMustFitAndHaveSomeone() {
        assertNull(Split.byItems(100, listOf(1), listOf(SplitItem(101, setOf(1)))))
        assertNull(Split.byItems(100, listOf(1), listOf(SplitItem(10, emptySet()))))
    }

    @Test
    fun requestCarriesAUpiLinkOnlyWithAnId() {
        val owed = listOf(Owed("Dev", 78_000), Owed("Kabir", 78_000))
        assertEquals(
            "Your share of Biryani:\nDev: ₹780\nKabir: ₹780",
            paymentRequest(owed, "Biryani", "", "Aarav")
        )
        assertTrue(
            paymentRequest(owed, "Biryani", "a@upi", "Aarav M")
                .endsWith("Pay: upi://pay?pa=a%40upi&pn=Aarav%20M&am=780&cu=INR&tn=Biryani")
        )
    }
}
