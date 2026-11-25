package com.saadm.zenith.domain.model

import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/** One budget or reporting month. [startMillis] is inclusive and [endMillis] exclusive. */
data class MonthPeriod(
    val startDate: LocalDate,
    val endDate: LocalDate,
    val startMillis: Long,
    val endMillis: Long
) {
    val days: Int get() = ChronoUnit.DAYS.between(startDate, endDate).toInt()

    operator fun contains(epochMillis: Long): Boolean = epochMillis >= startMillis && epochMillis < endMillis
}

/** Month boundaries for a month that starts on [startDay] (1..28, from the settings). */
object MonthPeriods {

    fun containing(date: LocalDate, startDay: Int = 1, zone: ZoneId = ZoneId.systemDefault()): MonthPeriod {
        val day = MonthStartDay.coerce(startDay)
        val start = if (date.dayOfMonth >= day) date.withDayOfMonth(day) else date.minusMonths(1).withDayOfMonth(day)
        return of(start, zone)
    }

    fun previous(period: MonthPeriod, zone: ZoneId = ZoneId.systemDefault()): MonthPeriod =
        of(period.startDate.minusMonths(1), zone)

    fun next(period: MonthPeriod, zone: ZoneId = ZoneId.systemDefault()): MonthPeriod =
        of(period.endDate, zone)

    private fun of(start: LocalDate, zone: ZoneId): MonthPeriod {
        val end = start.plusMonths(1)
        return MonthPeriod(
            startDate = start,
            endDate = end,
            startMillis = start.atStartOfDay(zone).toInstant().toEpochMilli(),
            endMillis = end.atStartOfDay(zone).toInstant().toEpochMilli()
        )
    }
}
