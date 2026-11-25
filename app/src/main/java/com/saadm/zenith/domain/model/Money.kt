package com.saadm.zenith.domain.model

import java.math.BigDecimal
import java.math.RoundingMode

private const val RUPEE = "₹"
private const val MINUS = "−"

/** An amount split so the UI can mute the cents. [sign] is "", "+" or U+2212. */
data class FormattedMoney(val sign: String, val whole: String, val cents: String?) {
    val text: String get() = "$sign$RUPEE$whole" + (cents?.let { ".$it" } ?: "")

    val spoken: String get() {
        val lead = when (sign) {
            MINUS -> "minus "
            "+" -> "plus "
            else -> ""
        }
        val paise = cents?.let { " and ${it.toInt()} paise" } ?: ""
        return "${lead}rupees $whole$paise"
    }
}

object MoneyFormatter {

    /** Amounts are paise (Long). [signed] adds a leading + to gains; losses always get U+2212. */
    fun format(paise: Long, signed: Boolean = false): FormattedMoney {
        val sign = when {
            paise < 0 -> MINUS
            signed && paise > 0 -> "+"
            else -> ""
        }
        // Work on the digits rather than Math.abs, so Long.MIN_VALUE cannot overflow.
        val digits = paise.toString().removePrefix("-").padStart(3, '0')
        val whole = digits.dropLast(2).trimStart('0').ifEmpty { "0" }
        val cents = digits.takeLast(2).takeIf { it != "00" }
        return FormattedMoney(sign, groupIndian(whole), cents)
    }

    /** Typed rupees to paise, ignoring grouping commas. Null unless it is above zero with at most two decimals. */
    fun parse(input: String): Long? {
        val text = input.trim().replace(",", "")
        // Twelve whole digits keeps the paise well inside a Long.
        if (!Regex("\\d{0,12}(\\.\\d{0,2})?").matches(text) || text.none { it.isDigit() }) return null
        return BigDecimal(text).movePointRight(2).longValueExact().takeIf { it > 0 }
    }

    /** Paise as the plain text a field is prefilled with: no symbol, no grouping. */
    fun plain(paise: Long): String =
        BigDecimal.valueOf(paise, 2).let { if (paise % 100 == 0L) it.setScale(0) else it }.toPlainString()

    /** Short form for axes and tight labels, e.g. 24.3k. Rounds half-up. */
    fun compact(paise: Long): String {
        val sign = if (paise < 0) MINUS else ""
        val rupees = BigDecimal.valueOf(paise).movePointLeft(2).abs()
        var unit = SCALES.indexOfLast { rupees >= it.divisor }.coerceAtLeast(0)
        var scaled = scaled(rupees, unit)
        // 999.5 rounds up to 1000, so it moves to the next unit.
        if (scaled >= BigDecimal(1000) && unit < SCALES.lastIndex) {
            unit++
            scaled = scaled(rupees, unit)
        }
        return sign + scaled.stripTrailingZeros().toPlainString() + SCALES[unit].suffix
    }

    private fun scaled(rupees: BigDecimal, unit: Int): BigDecimal {
        val decimals = if (unit == 0) 0 else 1
        return rupees.divide(SCALES[unit].divisor).setScale(decimals, RoundingMode.HALF_UP)
    }

    // Indian grouping: the last three digits, then pairs. 107070 -> 1,07,070
    private fun groupIndian(whole: String): String {
        if (whole.length <= 3) return whole
        val pairs = whole.dropLast(3).reversed().chunked(2).map { it.reversed() }.reversed()
        return pairs.joinToString(",") + "," + whole.takeLast(3)
    }

    private data class Scale(val divisor: BigDecimal, val suffix: String)

    private val SCALES = listOf(
        Scale(BigDecimal.ONE, ""),
        Scale(BigDecimal(1_000), "k"),
        Scale(BigDecimal(1_000_000), "M"),
        Scale(BigDecimal(1_000_000_000), "B"),
    )
}
