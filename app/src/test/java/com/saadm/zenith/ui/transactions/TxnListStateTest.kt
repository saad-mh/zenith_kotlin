package com.saadm.zenith.ui.transactions

import com.saadm.zenith.data.entity.TransactionEntity
import com.saadm.zenith.data.entity.TxnType
import com.saadm.zenith.domain.model.MonthPeriods
import com.saadm.zenith.domain.model.TxnFilter
import com.saadm.zenith.domain.transaction.TxnItems
import com.saadm.zenith.domain.transaction.TxnNames
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class TxnListStateTest {

    private val period = MonthPeriods.containing(LocalDate.of(2026, 10, 20))

    private fun at(month: Int, day: Int) =
        LocalDate.of(2026, month, day).atTime(12, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

    private fun txn(id: Long, type: TxnType, amount: Long, time: Long, payee: Long? = null, created: Long = id) =
        TransactionEntity(
            id = id, amount = amount, type = type, transactedAt = time, createdAt = created, updatedAt = created,
            categoryId = 1, payeeId = payee, accountId = null, note = null, receiptUri = null
        )

    private val items = TxnItems.of(
        listOf(
            txn(1, TxnType.EXPENSE, 100, at(10, 5), created = 9),
            txn(2, TxnType.DUE_TO, 40, at(10, 5), payee = 7, created = 9),
            txn(3, TxnType.SETTLEMENT_FROM, 40, at(10, 4), payee = 7),
            txn(4, TxnType.EXPENSE, 10, at(9, 30)),
        ),
        TxnNames()
    )

    @Test
    fun groupsTheMonthByDayWithSpendingTotals() {
        val days = groupByDay(items, period, TxnQuery(), null)
        assertEquals(listOf(5, 4), days.map { it.date.dayOfMonth })
        assertEquals(-100L, days[0].total)
        assertNull(days[1].total)
    }

    @Test
    fun theyOweFindsASplitByItsShare() {
        val days = groupByDay(items, period, TxnQuery(filter = TxnFilter.OWES_YOU), null)
        assertEquals(listOf(1L), days.flatMap { it.items }.map { it.txn.id })
    }

    @Test
    fun searchStaysInTheMonthUntilAskedForAll() {
        val query = TxnQuery(query = "x")
        assertEquals(1, groupByDay(items, period, query, setOf(2L, 4L)).size)
        assertEquals(2, groupByDay(items, period, query.copy(allMonths = true), setOf(2L, 4L)).size)
    }
}
