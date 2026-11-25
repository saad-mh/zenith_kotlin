package com.saadm.zenith.ui.budgets

import com.saadm.zenith.domain.budget.BudgetStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class BudgetQuipsTest {
    @Test
    fun everyLineFillsInTheAmountAndAnyKeyWorks() {
        BudgetStatus.entries.forEach { status ->
            (-3..60).forEach { key -> assertFalse(budgetQuip(status, key, "₹40").contains("{x}")) }
        }
        assertEquals("Looking good, money boss", budgetQuip(BudgetStatus.ON_TRACK, 0, "₹40"))
        assertEquals("Over by ₹40... impressive", budgetQuip(BudgetStatus.OVER, 15, "₹40"))
    }
}
