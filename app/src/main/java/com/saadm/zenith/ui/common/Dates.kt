package com.saadm.zenith.ui.common

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale

fun Long.toLocalDate(zone: ZoneId = ZoneId.systemDefault()): LocalDate =
    Instant.ofEpochMilli(this).atZone(zone).toLocalDate()

/** "October" */
fun LocalDate.monthName(): String = month.getDisplayName(TextStyle.FULL, Locale.ENGLISH)

/** "Oct" */
fun LocalDate.shortMonthName(): String = month.getDisplayName(TextStyle.SHORT, Locale.ENGLISH)
