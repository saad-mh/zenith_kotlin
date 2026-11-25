package com.saadm.zenith.domain.split

import com.saadm.zenith.domain.model.MoneyFormatter
import java.net.URLEncoder

/** The id that stands for the user among an item's participants. Payee ids start at 1. */
const val YOU = 0L

/** One line of a bill and who shares it. [participants] holds payee ids and, for the user, [YOU]. */
data class SplitItem(val amount: Long, val participants: Set<Long>)

/**
 * What each other person owes, in paise. The user's share is the total minus these, so every split
 * adds up exactly and odd paise stay with the user. Null means the figures don't fit the total.
 */
object Split {

    fun equal(total: Long, others: List<Long>): Map<Long, Long> =
        others.associateWith { total / (others.size + 1) }

    fun custom(total: Long, amounts: Map<Long, Long>): Map<Long, Long>? =
        amounts.takeIf { it.values.all { amount -> amount >= 0 } && it.values.sum() <= total }

    /** Each item is shared by its participants. Whatever the items leave over (tax, tip) is shared by all. */
    fun byItems(total: Long, others: List<Long>, items: List<SplitItem>): Map<Long, Long>? {
        val listed = items.sumOf { it.amount }
        if (listed > total || items.any { it.amount < 0 || it.participants.isEmpty() }) return null
        val owed = equal(total - listed, others).toMutableMap()
        items.forEach { item ->
            val each = item.amount / item.participants.size
            // If the user isn't on the item, the odd paise go to the first person on it.
            var odd = if (YOU in item.participants) 0L else item.amount - each * item.participants.size
            item.participants.filter { it != YOU }.forEach { person ->
                owed[person] = (owed[person] ?: 0L) + each + odd
                odd = 0L
            }
        }
        return owed
    }
}

/** One person's part of a request message. */
data class Owed(val name: String, val amount: Long)

/**
 * A message asking for money back, to hand to the share sheet. With a UPI ID it carries a `upi://`
 * link; the amount is filled in only when everyone owes the same. Nothing is paid inside the app.
 */
fun paymentRequest(owed: List<Owed>, about: String, upiId: String, payeeName: String): String {
    val lines = owed.joinToString("\n") { "${it.name}: ${MoneyFormatter.format(it.amount).text}" }
    val message = "Your share of $about:\n$lines"
    if (upiId.isBlank()) return message
    fun encode(value: String) = URLEncoder.encode(value, "UTF-8").replace("+", "%20")
    val amount = owed.map { it.amount }.distinct().singleOrNull()
        ?.let { "&am=${MoneyFormatter.plain(it)}" }.orEmpty()
    val name = payeeName.ifBlank { upiId }
    return "$message\nPay: upi://pay?pa=${encode(upiId.trim())}&pn=${encode(name)}$amount&cu=INR&tn=${encode(about)}"
}
