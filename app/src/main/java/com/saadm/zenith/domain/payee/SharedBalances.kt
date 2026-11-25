package com.saadm.zenith.domain.payee

import com.saadm.zenith.domain.model.MoneyFormatter

/** One payee's position, for the summary lists. */
data class PayeeBalanceLine(
    val payeeId: Long,
    val name: String,
    val net: Long,
    val oldestOpenAt: Long?
) {
    val direction: BalanceDirection
        get() = when {
            net > 0 -> BalanceDirection.OWES_YOU
            net < 0 -> BalanceDirection.YOU_OWE
            else -> BalanceDirection.SETTLED
        }
}

/**
 * Across everyone: what is owed to you, what you owe, the net, how many people sit on each side,
 * and the open balances oldest first.
 */
data class SharedBalanceSummary(
    val owedToYou: Long,
    val youOwe: Long,
    val net: Long,
    val peopleOwingYou: Int,
    val peopleYouOwe: Int,
    val oldestFirst: List<PayeeBalanceLine>
)

object SharedBalances {

    fun summarize(lines: List<PayeeBalanceLine>): SharedBalanceSummary {
        val owing = lines.filter { it.direction == BalanceDirection.OWES_YOU }
        val owed = lines.filter { it.direction == BalanceDirection.YOU_OWE }
        return SharedBalanceSummary(
            owedToYou = owing.sumOf { it.net },
            youOwe = owed.sumOf { -it.net },
            net = lines.sumOf { it.net },
            peopleOwingYou = owing.size,
            peopleYouOwe = owed.size,
            oldestFirst = lines
                .filter { it.direction != BalanceDirection.SETTLED }
                .sortedWith(compareBy<PayeeBalanceLine> { it.oldestOpenAt ?: Long.MAX_VALUE }.thenBy { it.name })
        )
    }
}

/** A plain reminder for someone who owes the user. No network and nothing is sent. */
object PayeeReminder {

    /** Null when the payee owes nothing, or the user owes them: the reminder is only for money coming in. */
    fun textFor(name: String, net: Long): String? {
        if (net <= 0) return null
        return "Hi $name, a quick reminder that ${MoneyFormatter.format(net).text} is still outstanding. Thanks!"
    }
}
