package com.saadm.zenith.ui.add

import com.saadm.zenith.data.entity.TransactionEntity
import com.saadm.zenith.data.entity.TxnType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class AddFormTest {

    private val form = AddForm(LocalDate.of(2026, 10, 20))

    private fun typed(keys: String) = keys.fold("") { amount, key -> typeKey(amount, key.toString()) }

    @Test
    fun keypadKeepsAmountReadable() {
        assertEquals("12.34", typed("12.345"))
        assertEquals("0.5", typed(".5"))
        assertEquals("7", typed("07"))
        assertEquals("1.2", typed("1.2."))
        assertEquals("123456789", typed("1234567890"))
        assertEquals("1", typeKey("12", BACKSPACE))
    }

    @Test
    fun showsIndianGroupingAsTyped() {
        assertEquals("0", displayAmount(""))
        assertEquals("1,07,070", displayAmount("107070"))
        assertEquals("2,450.", displayAmount("2450."))
        assertEquals("2,450.50", displayAmount("2450.50"))
    }

    @Test
    fun saysWhatStopsASave() {
        assertEquals("Enter an amount", form.problem)
        assertNull(form.copy(amount = "10").problem)
        assertEquals("Choose a person", form.copy(amount = "10", type = TxnType.DUE_TO).problem)
    }

    @Test
    fun dropsIdsThatNoLongerExist() {
        val stale = form.copy(accountId = 1, payeeId = 2, people = listOf(2, 3))
        val known = stale.withKnownIds(accounts = setOf(9), payees = setOf(3))
        assertNull(known.accountId)
        assertNull(known.payeeId)
        assertEquals(listOf(3L), known.people)
    }

    @Test
    fun suggestsTheLatestCategoryForAMerchant() {
        fun row(note: String, category: Long, at: Long) = TransactionEntity(
            amount = 1, type = TxnType.EXPENSE, transactedAt = at, createdAt = at, updatedAt = at,
            categoryId = category, payeeId = null, accountId = null, note = note, receiptUri = null
        )
        val history = listOf(row("Third Wave", 1, 10), row("third wave coffee", 2, 20), row("Metro", 3, 30))
        assertEquals(2L, suggestCategory("Thi", history, setOf(1, 2, 3)))
        assertNull(suggestCategory("Th", history, setOf(1, 2, 3)))
        assertNull(suggestCategory("Metro", history, setOf(1)))
    }
}
