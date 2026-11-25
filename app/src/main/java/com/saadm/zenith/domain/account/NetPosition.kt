package com.saadm.zenith.domain.account

import com.saadm.zenith.data.entity.AccountBalance
import com.saadm.zenith.domain.model.MoneyFormatter
import com.saadm.zenith.domain.payee.SharedBalanceSummary

/**
 * What you are worth on paper: the sum of your accounts, plus the net of what friends owe you
 * and what you owe them.
 */
data class NetPosition(
    val accountsTotal: Long,
    val accountCount: Int,
    val friendsNet: Long
) {
    val net: Long get() = accountsTotal + friendsNet

    /** "₹1,02,340 in 4 accounts + ₹4,730 from friends", or "− ₹420 owed to friends" when the net is negative. */
    fun subtitle(): String {
        val noun = if (accountCount == 1) "account" else "accounts"
        val accounts = "${MoneyFormatter.format(accountsTotal).text} in $accountCount $noun"
        return when {
            friendsNet > 0 -> "$accounts + ${MoneyFormatter.format(friendsNet).text} from friends"
            friendsNet < 0 -> "$accounts − ${MoneyFormatter.format(-friendsNet).text} owed to friends"
            else -> "$accounts, friends settled"
        }
    }

    companion object {
        /** Active accounts only. Archived accounts are out of the figure. */
        fun of(accounts: List<AccountBalance>, shared: SharedBalanceSummary): NetPosition = NetPosition(
            accountsTotal = accounts.sumOf { it.balance },
            accountCount = accounts.size,
            friendsNet = shared.net
        )
    }
}
