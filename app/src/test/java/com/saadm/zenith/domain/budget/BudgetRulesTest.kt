package com.saadm.zenith.domain.budget

import com.saadm.zenith.data.entity.BudgetEntity
import com.saadm.zenith.data.entity.BudgetPeriod
import com.saadm.zenith.data.entity.TransactionEntity
import com.saadm.zenith.data.entity.TxnType
import com.saadm.zenith.domain.model.MonthPeriod
import com.saadm.zenith.domain.model.MonthPeriods
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class BudgetRulesTest {

    private val zone: ZoneId = ZoneId.of("Asia/Kolkata")
    private val february: MonthPeriod = MonthPeriods.containing(LocalDate.of(2026, 2, 1), zone = zone)

    @Test
    fun thresholdsSplitOnTrackNearingAndOver() {
        assertEquals(BudgetStatus.ON_TRACK, BudgetRules.statusFor(spent = 8_499, limit = 10_000))
        assertEquals(BudgetStatus.NEARING, BudgetRules.statusFor(spent = 8_500, limit = 10_000))
        assertEquals(BudgetStatus.NEARING, BudgetRules.statusFor(spent = 10_000, limit = 10_000))
        assertEquals(BudgetStatus.OVER, BudgetRules.statusFor(spent = 10_001, limit = 10_000))
    }

    @Test
    fun paceIsDayOfPeriodOverDaysInPeriod() {
        assertEquals(0f, BudgetRules.pace(february, LocalDate.of(2026, 1, 31)), 0f)
        assertEquals(1f / 28, BudgetRules.pace(february, LocalDate.of(2026, 2, 1)), 1e-6f)
        assertEquals(14f / 28, BudgetRules.pace(february, LocalDate.of(2026, 2, 14)), 1e-6f)
        assertEquals(1f, BudgetRules.pace(february, LocalDate.of(2026, 3, 1)), 0f)
    }

    @Test
    fun perDayAllowanceSpreadsLeftOverDaysStillToCome() {
        // Feb 14 counts as a day still to come, so 14th to 28th inclusive is 15 days.
        assertEquals(933L, BudgetRules.perDayAllowance(left = 14_000, february, LocalDate.of(2026, 2, 14)))
        assertEquals(1_000L, BudgetRules.perDayAllowance(left = 1_000, february, LocalDate.of(2026, 2, 28)))
        assertEquals(0L, BudgetRules.perDayAllowance(left = -500, february, LocalDate.of(2026, 2, 14)))
        assertNull(BudgetRules.perDayAllowance(left = 500, february, LocalDate.of(2026, 3, 2)))
    }

    @Test
    fun suggestionsRoundToTheNearestHundredRupeesFromAThousandUp() {
        assertEquals(0L, BudgetRules.roundSuggestion(0))
        assertEquals(5_000L, BudgetRules.roundSuggestion(4_500))       // ₹45 -> ₹50
        assertEquals(120_000L, BudgetRules.roundSuggestion(123_456))   // ₹1,234.56 -> ₹1,200
        assertEquals(10_000L, BudgetRules.roundSuggestion(99_99))      // ₹99.99 -> ₹100
        assertEquals(100_000L, BudgetRules.roundSuggestion(100_000))   // ₹1,000 stays
        assertEquals(200_000L, BudgetRules.roundSuggestion(195_000))   // ₹1,950 -> ₹2,000
    }

    @Test
    fun pastMonthsAreReadOnly() {
        assertTrue(BudgetRules.isEditable(february, LocalDate.of(2026, 2, 27)))
        assertTrue(BudgetRules.isEditable(february, LocalDate.of(2026, 1, 5)))
        assertFalse(BudgetRules.isEditable(february, LocalDate.of(2026, 3, 1)))
    }

    @Test
    fun summaryCountsOnlyExpenseAndPutsLimitsInTheirStatus() {
        val budgets = listOf(
            budget(categoryId = null, limit = 10_000_00, period = february),
            budget(categoryId = 3L, limit = 2_000_00, period = february),
        )
        val txns = listOf(
            txn(TxnType.EXPENSE, 9_000_00, categoryId = 4L, date = LocalDate.of(2026, 2, 3)),
            txn(TxnType.EXPENSE, 2_500_00, categoryId = 3L, date = LocalDate.of(2026, 2, 4)),
            txn(TxnType.INCOME, 50_000_00, categoryId = 3L, date = LocalDate.of(2026, 2, 5)),
            txn(TxnType.DUE_FROM, 99_000_00, categoryId = 4L, date = LocalDate.of(2026, 2, 6)),
        )

        val month = BudgetRules.summarize(february, budgets, txns, LocalDate.of(2026, 2, 14))

        // Overall: 1,15,000 spent against 10,000 limit (in rupees). Food is only expense in category 3.
        assertEquals(BudgetStatus.OVER, month.overall!!.status)
        assertEquals(1_150_000L, month.overall!!.spent)
        assertEquals(BudgetStatus.OVER, month.categories.single().status)
        assertEquals(-50_000L, month.categories.single().left)
        assertEquals(0.5f, month.pace, 1e-6f)
        assertEquals(0L, month.perDayAllowance)
    }

    private fun budget(categoryId: Long?, limit: Long, period: MonthPeriod) = BudgetEntity(
        categoryId = categoryId,
        limitAmount = limit,
        periodType = BudgetPeriod.MONTHLY,
        startDate = period.startMillis,
        endDate = period.endMillis
    )

    private fun txn(type: TxnType, amount: Long, categoryId: Long, date: LocalDate) = TransactionEntity(
        amount = amount,
        type = type,
        transactedAt = date.atTime(12, 0).atZone(zone).toInstant().toEpochMilli(),
        createdAt = 0,
        updatedAt = 0,
        categoryId = categoryId,
        payeeId = null,
        accountId = null,
        note = null,
        receiptUri = null
    )
}
