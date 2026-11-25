package com.saadm.zenith.domain.transaction

import com.saadm.zenith.data.entity.TransactionEntity
import com.saadm.zenith.data.entity.TxnType
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class TxnItemsTest {

    private fun txn(id: Long, type: TxnType, amount: Long, payee: Long? = null, created: Long = id) =
        TransactionEntity(
            id = id, amount = amount, type = type, transactedAt = 1, createdAt = created, updatedAt = created,
            categoryId = 1, payeeId = payee, accountId = null, note = null, receiptUri = null
        )

    private val names = TxnNames(categories = mapOf(1L to "Food"), payees = mapOf(7L to "Dev", 8L to "Kabir"))

    @Test
    fun splitFoldsIntoOneItemWithYourShare() {
        val item = TxnItems.of(
            listOf(
                txn(1, TxnType.EXPENSE, 123_000, created = 5),
                txn(2, TxnType.DUE_TO, 41_000, payee = 7, created = 5),
                txn(3, TxnType.DUE_TO, 41_000, payee = 8, created = 5),
            ),
            names
        ).single()
        assertEquals(TxnTag.SHARED, item.tag)
        assertEquals(41_000L, item.yourShare)
        assertEquals(-123_000L, item.signedAmount)
        assertEquals(listOf("Dev", "Kabir"), item.people)
    }

    @Test
    fun dueWrittenOnItsOwnStaysItsOwnRow() {
        val items = TxnItems.of(
            listOf(txn(1, TxnType.EXPENSE, 100), txn(2, TxnType.DUE_TO, 50, payee = 7)),
            names
        )
        assertEquals(listOf(null, TxnTag.OWES_YOU), items.map { it.tag })
        assertEquals(listOf(-100L, 50L), items.map { it.signedAmount })
    }

    @Test
    fun dayLabels() {
        val today = LocalDate.of(2026, 10, 20)
        assertEquals("Today", dayLabel(today, today))
        assertEquals("Yesterday", dayLabel(today.minusDays(1), today))
        assertEquals("3 Oct", dayLabel(LocalDate.of(2026, 10, 3), today))
        assertEquals("3 Oct 2025", dayLabel(LocalDate.of(2025, 10, 3), today))
    }
}
