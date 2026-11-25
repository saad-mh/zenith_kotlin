package com.saadm.zenith.domain.model

/** How amounts are grouped: 1,07,070 (Indian) or 107,070 (international). */
enum class NumberFormat { INDIAN, INTERNATIONAL }

enum class WeekStart { MONDAY, SUNDAY }

/** How a new shared expense is divided between people. */
enum class SplitMode { EQUAL, BY_ITEMS, CUSTOM }

/** Month start days allowed. Capped at 28 so every month has that day. */
object MonthStartDay {
    const val MIN = 1
    const val MAX = 28
    fun coerce(day: Int): Int = day.coerceIn(MIN, MAX)
}
