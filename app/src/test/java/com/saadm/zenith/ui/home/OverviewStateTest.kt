package com.saadm.zenith.ui.home

import com.saadm.zenith.domain.analytics.CumulativeSpend
import com.saadm.zenith.domain.model.MonthPeriods
import com.saadm.zenith.domain.payee.PayeeBalanceLine
import com.saadm.zenith.domain.payee.SharedBalances
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class OverviewStateTest {

    private val today = LocalDate.of(2026, 10, 3)

    @Test
    fun comparesWithLastMonthOnTheSameDay() {
        val spend = MonthSpend.of(
            MonthPeriods.containing(today),
            CumulativeSpend(thisMonth = listOf(10, 20, 30, 30), previousMonth = listOf(5, 50, 70, 90)),
            today
        )
        assertEquals(30L, spend.spent)
        assertEquals(-40L, spend.difference)
        assertEquals(listOf(10L, 20L, 30L), spend.thisSoFar)
    }

    @Test
    fun noNudgeWhenOnlyYouOwe() {
        val owedByYou = SharedBalances.summarize(listOf(PayeeBalanceLine(1, "A", -100, 0)))
        assertNull(oldestNudge(owedByYou, today))
    }
}
