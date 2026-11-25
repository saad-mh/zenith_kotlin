package com.saadm.zenith.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class MoneyFormatterTest {

    private fun check(expected: String, actual: String) = assertEquals(expected, actual)

    @Test
    fun formatsPlainAmountsWithCentsHiddenWhenZero() {
        check("₹0", MoneyFormatter.format(0).text)
        check("₹0.01", MoneyFormatter.format(1).text)
        check("₹0.05", MoneyFormatter.format(5).text)
        check("₹1", MoneyFormatter.format(100).text)
        check("₹420", MoneyFormatter.format(42_000).text)
        check("₹420.50", MoneyFormatter.format(42_050).text)
    }

    @Test
    fun groupsDigitsTheIndianWay() {
        check("₹12,000", MoneyFormatter.format(1_200_000).text)
        check("₹1,07,070", MoneyFormatter.format(10_707_000).text)
        check("₹1,00,00,000", MoneyFormatter.format(1_000_000_000).text)
        check("₹10,00,00,000", MoneyFormatter.format(10_000_000_000).text)
        check("₹92,23,37,20,36,85,47,758.07", MoneyFormatter.format(Long.MAX_VALUE).text)
    }

    @Test
    fun negativeUsesTrueMinusSign() {
        check("−₹420", MoneyFormatter.format(-42_000).text)
        check("−₹0.01", MoneyFormatter.format(-1).text)
    }

    @Test
    fun signedShowsPlusOnGainsOnly() {
        check("+₹12,000", MoneyFormatter.format(1_200_000, signed = true).text)
        check("₹0", MoneyFormatter.format(0, signed = true).text)
        check("−₹420", MoneyFormatter.format(-42_000, signed = true).text)
    }

    @Test
    fun centsAreSplitOutForMuting() {
        val money = MoneyFormatter.format(107_050)
        check("1,070", money.whole)
        check("50", money.cents!!)
        assertEquals(null, MoneyFormatter.format(42_000).cents)
    }

    @Test
    fun spokenFormReadsSignsAsWords() {
        check("minus rupees 420", MoneyFormatter.format(-42_000).spoken)
        check("plus rupees 12,000", MoneyFormatter.format(1_200_000, signed = true).spoken)
        check("rupees 1,070 and 50 paise", MoneyFormatter.format(107_050).spoken)
        check("rupees 0", MoneyFormatter.format(0).spoken)
    }

    @Test
    fun compactUsesShortSuffixesAndRoundsHalfUp() {
        check("0", MoneyFormatter.compact(0))
        check("100", MoneyFormatter.compact(9_999))
        check("1k", MoneyFormatter.compact(100_000))
        check("1k", MoneyFormatter.compact(99_950))
        check("24.3k", MoneyFormatter.compact(2_430_000))
        check("24.3k", MoneyFormatter.compact(2_434_000))
        check("24.4k", MoneyFormatter.compact(2_435_000))
        check("420k", MoneyFormatter.compact(42_000_000))
        check("1M", MoneyFormatter.compact(99_999_500))
        check("1.5M", MoneyFormatter.compact(150_000_000))
        check("10B", MoneyFormatter.compact(1_000_000_000_000))
        check("−24.3k", MoneyFormatter.compact(-2_430_000))
    }
}
