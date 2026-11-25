package com.saadm.zenith.ui.budgets

import com.saadm.zenith.domain.budget.BudgetLine
import com.saadm.zenith.domain.budget.BudgetStatus
import com.saadm.zenith.domain.budget.MonthBudgets
import com.saadm.zenith.domain.model.MonthPeriods
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class BudgetSummaryTest {

    private val period = MonthPeriods.containing(LocalDate.of(2026, 10, 20))

    private fun month(overall: BudgetLine?, vararg lines: BudgetLine) =
        MonthBudgets(period, overall, lines.toList(), 0.5f, null, null, true)

    private fun line(limit: Long, spent: Long, id: Long? = 1) = BudgetLine(id, limit, spent, BudgetStatus.ON_TRACK)

    @Test
    fun addsCategoryBudgetsWhenThereIsNoOverallOne() {
        val summary = BudgetSummary.of(month(null, line(1000, 200), line(1000, 300)))!!
        assertEquals(1500L, summary.left)
        assertEquals(500L, summary.underPace)
        assertEquals("On pace", summary.status)
    }

    @Test
    fun overallBudgetWins() {
        val summary = BudgetSummary.of(month(line(1000, 900, null), line(100, 10)))!!
        assertEquals("Ahead of pace", summary.status)
        assertEquals(100L, summary.left)
    }

    @Test
    fun noBudgetsNoSummary() {
        assertNull(BudgetSummary.of(month(null)))
    }
}
