package com.saadm.zenith.domain.payee

import com.saadm.zenith.data.entity.TransactionEntity
import com.saadm.zenith.data.entity.TxnType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneOffset

class StatementTest {

    private fun row(id: Long, type: TxnType, amount: Long) = TransactionEntity(
        id = id, amount = amount, type = type, transactedAt = id * 86_400_000, createdAt = id, updatedAt = id,
        categoryId = 1, payeeId = 1, accountId = null, note = null, receiptUri = null
    )

    private fun statement(vararg rows: TransactionEntity, notes: Map<Long, String?> = emptyMap()) =
        balanceStatement("Dev", PayeeLedger.of(rows.toList()), notes, ZoneOffset.UTC)

    @Test
    fun saysWhoOwesWhom() {
        assertEquals(
            "Hi Dev,\nYou owe me ₹320.\n\nRecent:\n2 Jan 1970 · You owe ₹320 · Cab",
            statement(row(1, TxnType.DUE_TO, 32_000), notes = mapOf(1L to " Cab "))
        )
        assertTrue(statement(row(1, TxnType.DUE_FROM, 500)).startsWith("Hi Dev,\nI owe you ₹5."))
    }

    @Test
    fun settledAndEmptyHistories() {
        assertEquals("Hi Dev,\nWe're all settled.", statement())
        val settled = statement(row(1, TxnType.DUE_TO, 500), row(2, TxnType.SETTLEMENT_FROM, 500))
        assertTrue(settled.contains("We're all settled.") && settled.contains("You paid ₹5"))
    }

    @Test
    fun keepsTheTenNewestAndCountsTheRest() {
        val text = statement(*(1L..12L).map { row(it, TxnType.DUE_TO, 100) }.toTypedArray())
        assertTrue(text.contains("You owe me ₹12."))
        assertTrue(text.contains("13 Jan 1970"))
        assertTrue(text.lines().none { it.startsWith("3 Jan 1970") })
        assertEquals(10, text.lines().count { " · You owe " in it })
        assertTrue(text.endsWith("(2 earlier not shown)"))
    }

    @Test
    fun boldMarksOnlyTheBalanceAndTheHeading() {
        fun bold(vararg rows: TransactionEntity, notes: Map<Long, String?> = emptyMap()) =
            balanceStatement("Dev", PayeeLedger.of(rows.toList()), notes, ZoneOffset.UTC, bold = true)
        // Owed to me, with a note: the name and the note stay as typed.
        assertEquals(
            "Hi Dev,\n*You owe me ₹320.*\n\n*Recent:*\n2 Jan 1970 · You owe ₹320 · Cab",
            bold(row(1, TxnType.DUE_TO, 32_000), notes = mapOf(1L to "Cab"))
        )
        assertTrue(bold(row(1, TxnType.DUE_FROM, 500)).startsWith("Hi Dev,\n*I owe you ₹5.*\n"))
        assertEquals("Hi Dev,\n*We're all settled.*", bold())
    }
}
