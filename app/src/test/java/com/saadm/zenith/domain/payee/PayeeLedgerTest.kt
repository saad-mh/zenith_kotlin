package com.saadm.zenith.domain.payee

import com.saadm.zenith.data.entity.TransactionEntity
import com.saadm.zenith.data.entity.TxnType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PayeeLedgerTest {

    private var nextId = 1L

    private fun item(type: TxnType, amount: Long, at: Long, deleted: Boolean = false) = TransactionEntity(
        id = nextId++,
        amount = amount,
        type = type,
        transactedAt = at,
        createdAt = at,
        updatedAt = at,
        categoryId = 3L,
        payeeId = 1L,
        accountId = null,
        note = null,
        receiptUri = null,
        isDeleted = deleted
    )

    @Test
    fun partialSettlementLeavesTheRemainderOpen() {
        val due = item(TxnType.DUE_TO, 10_000, at = 100)
        val paid = item(TxnType.SETTLEMENT_FROM, 4_000, at = 200)

        val ledger = PayeeLedger.of(listOf(due, paid))

        assertEquals(6_000L, ledger.net)
        assertEquals(BalanceDirection.OWES_YOU, ledger.direction)
        assertEquals(100L, ledger.oldestOpenAt)
        assertEquals(1, ledger.openDueCount)
    }

    @Test
    fun overpaymentFlipsTheDirection() {
        val due = item(TxnType.DUE_TO, 5_000, at = 100)
        val paid = item(TxnType.SETTLEMENT_FROM, 8_000, at = 200)

        val ledger = PayeeLedger.of(listOf(due, paid))

        assertEquals(-3_000L, ledger.net)
        assertEquals(BalanceDirection.YOU_OWE, ledger.direction)
        assertEquals(100L, ledger.oldestOpenAt)
    }

    @Test
    fun settlingToZeroClosesTheBalance() {
        val ledger = PayeeLedger.of(
            listOf(
                item(TxnType.DUE_TO, 10_000, at = 100),
                item(TxnType.SETTLEMENT_FROM, 10_000, at = 200),
            )
        )

        assertEquals(0L, ledger.net)
        assertEquals(BalanceDirection.SETTLED, ledger.direction)
        assertNull(ledger.oldestOpenAt)
        assertEquals(0, ledger.openDueCount)
        assertEquals(listOf(10_000L, 0L), ledger.history.map { it.runningBalance })
    }

    @Test
    fun interleavedItemsFromBothDirectionsRunAsOneBalance() {
        val ledger = PayeeLedger.of(
            listOf(
                item(TxnType.DUE_TO, 10_000, at = 100),
                item(TxnType.DUE_FROM, 3_000, at = 200),
                item(TxnType.SETTLEMENT_FROM, 5_000, at = 300),
                item(TxnType.DUE_TO, 2_000, at = 400),
            )
        )

        assertEquals(listOf(10_000L, 7_000L, 2_000L, 4_000L), ledger.history.map { it.runningBalance })
        assertEquals(4_000L, ledger.net)
        assertEquals(BalanceDirection.OWES_YOU, ledger.direction)
        assertEquals(3, ledger.openDueCount)
    }

    @Test
    fun aReopenedDebtDatesFromTheReopening() {
        val ledger = PayeeLedger.of(
            listOf(
                item(TxnType.DUE_TO, 1_000, at = 100),
                item(TxnType.SETTLEMENT_FROM, 1_000, at = 200),
                item(TxnType.DUE_FROM, 500, at = 300),
            )
        )

        assertEquals(-500L, ledger.net)
        assertEquals(300L, ledger.oldestOpenAt)
        assertEquals(1, ledger.openDueCount)
    }

    @Test
    fun itemsAreReadInTimeOrderNotInsertionOrder() {
        val later = item(TxnType.SETTLEMENT_FROM, 400, at = 200)
        val earlier = item(TxnType.DUE_TO, 1_000, at = 100)

        val ledger = PayeeLedger.of(listOf(later, earlier))

        assertEquals(listOf(1_000L, 600L), ledger.history.map { it.runningBalance })
    }

    @Test
    fun deletedRowsAndOtherTypesAreIgnored() {
        val ledger = PayeeLedger.of(
            listOf(
                item(TxnType.DUE_TO, 1_000, at = 100),
                item(TxnType.DUE_TO, 9_999, at = 150, deleted = true),
                item(TxnType.EXPENSE, 9_999, at = 160),
            )
        )

        assertEquals(1_000L, ledger.net)
        assertEquals(1, ledger.history.size)
        assertTrue(ledger.direction == BalanceDirection.OWES_YOU)
    }

    @Test
    fun reminderOnlyAsksForMoneyComingIn() {
        assertEquals(
            "Hi Asha, a quick reminder that ₹60 is still outstanding. Thanks!",
            PayeeReminder.textFor("Asha", 6_000)
        )
        assertNull(PayeeReminder.textFor("Asha", 0))
        assertNull(PayeeReminder.textFor("Asha", -3_000))
    }

    @Test
    fun sharedSummaryCountsBothSidesAndSortsOldestFirst() {
        val summary = SharedBalances.summarize(
            listOf(
                PayeeBalanceLine(payeeId = 1, name = "Asha", net = 6_000, oldestOpenAt = 500),
                PayeeBalanceLine(payeeId = 2, name = "Bilal", net = -3_000, oldestOpenAt = 100),
                PayeeBalanceLine(payeeId = 3, name = "Chitra", net = 0, oldestOpenAt = null),
            )
        )

        assertEquals(6_000L, summary.owedToYou)
        assertEquals(3_000L, summary.youOwe)
        assertEquals(3_000L, summary.net)
        assertEquals(1, summary.peopleOwingYou)
        assertEquals(1, summary.peopleYouOwe)
        assertEquals(listOf("Bilal", "Asha"), summary.oldestFirst.map { it.name })
    }
}
