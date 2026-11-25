package com.saadm.zenith.ui.people

import com.saadm.zenith.domain.model.Payee
import com.saadm.zenith.domain.payee.BalanceDirection
import com.saadm.zenith.domain.payee.PayeeLedger
import org.junit.Assert.assertEquals
import org.junit.Test

class PayeeSectionsTest {

    private fun row(name: String, net: Long) = PayeeRow(
        Payee(name.hashCode().toLong(), name, null, null, null, 0),
        PayeeLedger(
            net,
            when {
                net > 0 -> BalanceDirection.OWES_YOU
                net < 0 -> BalanceDirection.YOU_OWE
                else -> BalanceDirection.SETTLED
            },
            null, 0, emptyList()
        )
    )

    private val rows = listOf(row("Dev", 100), row("Kabir", 50), row("Isha", -30), row("Anu", 0))

    @Test
    fun groupsByDirectionWithSubtotals() {
        val sections = payeeSections(rows, PayeeFilter.All, "")
        assertEquals(listOf("Owes you", "You owe", "Settled"), sections.map { it.title })
        assertEquals(listOf(150L, -30L, 0L), sections.map { it.subtotal })
    }

    @Test
    fun filtersAndSearches() {
        assertEquals(listOf("You owe"), payeeSections(rows, PayeeFilter.YouOwe, "").map { it.title })
        assertEquals(listOf("Kabir"), payeeSections(rows, PayeeFilter.All, " kab ").single().rows.map { it.payee.name })
    }

    @Test
    fun settlementMovesTheBalanceTowardsZero() {
        assertEquals(20L, balanceAfter(100, theyPaidYou = true, amount = 80))
        assertEquals(0L, balanceAfter(-30, theyPaidYou = false, amount = 30))
    }
}
