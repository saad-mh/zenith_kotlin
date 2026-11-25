package com.saadm.zenith.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class MonthPeriodTest {

    private val kolkata: ZoneId = ZoneId.of("Asia/Kolkata")

    private fun period(date: LocalDate, startDay: Int) = MonthPeriods.containing(date, startDay, kolkata)

    @Test
    fun calendarMonthWhenStartDayIsOne() {
        val p = period(LocalDate.of(2026, 2, 15), startDay = 1)
        assertEquals(LocalDate.of(2026, 2, 1), p.startDate)
        assertEquals(LocalDate.of(2026, 3, 1), p.endDate)
        assertEquals(28, p.days)
    }

    @Test
    fun dateBeforeStartDayBelongsToPreviousPeriod() {
        val p = period(LocalDate.of(2026, 2, 10), startDay = 15)
        assertEquals(LocalDate.of(2026, 1, 15), p.startDate)
        assertEquals(LocalDate.of(2026, 2, 15), p.endDate)
    }

    @Test
    fun startDayItselfBelongsToTheNewPeriod() {
        val p = period(LocalDate.of(2026, 2, 15), startDay = 15)
        assertEquals(LocalDate.of(2026, 2, 15), p.startDate)
        assertEquals(LocalDate.of(2026, 3, 15), p.endDate)
    }

    @Test
    fun startDayIsCappedAt28() {
        val p = period(LocalDate.of(2026, 2, 27), startDay = 31)
        assertEquals(LocalDate.of(2026, 1, 28), p.startDate)
    }

    @Test
    fun boundariesAreStartInclusiveEndExclusive() {
        val p = period(LocalDate.of(2026, 2, 10), startDay = 1)
        val startOfDay = LocalDate.of(2026, 2, 1).atStartOfDay(kolkata).toInstant().toEpochMilli()
        val endOfDay = LocalDate.of(2026, 3, 1).atStartOfDay(kolkata).toInstant().toEpochMilli()

        assertTrue(startOfDay in p)
        assertFalse(startOfDay - 1 in p)
        assertTrue(endOfDay - 1 in p)
        assertFalse(endOfDay in p)
    }

    @Test
    fun previousAndNextStepOneMonth() {
        val feb = period(LocalDate.of(2026, 2, 20), startDay = 15)
        val prev = MonthPeriods.previous(feb, kolkata)
        val next = MonthPeriods.next(feb, kolkata)

        assertEquals(LocalDate.of(2026, 1, 15), prev.startDate)
        assertEquals(LocalDate.of(2026, 3, 15), next.startDate)
        assertEquals(feb.endMillis, next.startMillis)
    }
}
