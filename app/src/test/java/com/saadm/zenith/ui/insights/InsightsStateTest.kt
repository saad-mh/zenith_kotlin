package com.saadm.zenith.ui.insights

import com.saadm.zenith.domain.analytics.CategoryTotal
import com.saadm.zenith.domain.budget.BudgetLine
import com.saadm.zenith.domain.budget.BudgetStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class InsightsStateTest {

    @Test
    fun smallCategoriesGroupAsEverythingElse() {
        val totals = (1L..6L).map { CategoryTotal(it, it * 10) }
        val top = topCategories(totals, emptyMap())
        assertEquals(5, top.size)
        assertEquals("Everything else" to 30L, top.last())
        assertEquals(5, topCategories(totals.take(5), emptyMap()).size)
        assertEquals("Uncategorized" to 50L, topCategories(totals.take(5), emptyMap()).first())
    }

    @Test
    fun worstBudgetsAreOverThenNearing() {
        val lines = listOf(
            BudgetLine(1, 100, 10, BudgetStatus.ON_TRACK),
            BudgetLine(2, 100, 90, BudgetStatus.NEARING),
            BudgetLine(3, 100, 120, BudgetStatus.OVER),
            BudgetLine(4, 100, 86, BudgetStatus.NEARING),
        )
        assertEquals(listOf(3L, 2L), worstBudgets(lines).map { it.categoryId })
    }
}
