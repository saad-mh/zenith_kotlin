package com.saadm.zenith.domain.account

import com.saadm.zenith.data.entity.AccountBalance
import com.saadm.zenith.data.entity.AccountType
import com.saadm.zenith.domain.payee.SharedBalanceSummary
import org.junit.Assert.assertEquals
import org.junit.Test

class NetPositionTest {

    private fun account(balance: Long) = AccountBalance(
        id = balance, name = "a", type = AccountType.BANK_TRANSFER, colorHex = "#000000", emoji = "x",
        sortOrder = 0, isDeleted = false, openingBalance = 0, balance = balance
    )

    private fun shared(net: Long) = SharedBalanceSummary(
        owedToYou = maxOf(net, 0), youOwe = maxOf(-net, 0), net = net,
        peopleOwingYou = 0, peopleYouOwe = 0, oldestFirst = emptyList()
    )

    @Test
    fun netIsAccountsPlusFriendsNet() {
        val position = NetPosition.of(listOf(account(10_234_000), account(5_000_00)), shared(473_000))
        assertEquals(10_234_000L + 500_000L, position.accountsTotal)
        assertEquals(2, position.accountCount)
        assertEquals(10_234_000L + 500_000L + 473_000L, position.net)
    }

    @Test
    fun subtitleNamesBothParts() {
        val position = NetPosition(accountsTotal = 10_234_000, accountCount = 4, friendsNet = 473_000)
        assertEquals("₹1,02,340 in 4 accounts + ₹4,730 from friends", position.subtitle())
    }

    @Test
    fun subtitleHandlesOwingFriendsAndOneAccount() {
        assertEquals(
            "₹1,000 in 1 account − ₹420 owed to friends",
            NetPosition(accountsTotal = 100_000, accountCount = 1, friendsNet = -42_000).subtitle()
        )
        assertEquals(
            "₹500 in 2 accounts, friends settled",
            NetPosition(accountsTotal = 50_000, accountCount = 2, friendsNet = 0).subtitle()
        )
    }
}
