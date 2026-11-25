package com.saadm.zenith.ui.add

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AmountExpressionTest {

    private fun typed(keys: String) = keys.fold("") { amount, key -> typeKey(amount, key.toString()) }

    @Test
    fun addsAndSubtractsInPaise() {
        assertEquals(15_550L, evaluate("120+45.50-10"))
        assertEquals(30L, evaluate("0.1+0.2"))
        assertEquals(500L, evaluate("5+0"))
    }

    @Test
    fun refusesIncompleteOrUnusableSums() {
        listOf("", "5+", "5-5", "5-9", "999999999+999999999").forEach { assertNull(it, evaluate(it)) }
        assertEquals("Enter an amount", amountProblem(""))
        assertEquals("Finish the sum", amountProblem("5+"))
        assertEquals("The total must be above zero", amountProblem("5-9"))
        assertEquals("That total is too large", amountProblem("999999999+999999999"))
        assertNull(amountProblem("5+1"))
    }

    @Test
    fun keypadKeepsEachFigureReadable() {
        assertEquals("12.34+5", typed("12.345+5"))
        assertEquals("5-", typed("5+-"))
        assertEquals("", typed("+-"))
        assertEquals("1.5+0.2", typed("1.5+.2"))
        assertEquals("7+8", typed("7+08"))
    }

    @Test
    fun showsGroupingAndTrueMinus() {
        assertEquals("1,07,070 + 5 − 1.50", displayAmount("107070+5-1.50"))
        assertEquals("2,450 +", displayAmount("2450+"))
    }
}
