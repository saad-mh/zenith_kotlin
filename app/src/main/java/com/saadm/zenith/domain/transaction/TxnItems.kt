package com.saadm.zenith.domain.transaction

import com.saadm.zenith.data.entity.TransactionEntity
import com.saadm.zenith.data.entity.TxnType
import com.saadm.zenith.domain.model.MoneyFormatter
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Names for the ids a transaction carries. */
data class TxnNames(
    val categories: Map<Long, String> = emptyMap(),
    val payees: Map<Long, String> = emptyMap(),
    val accounts: Map<Long, String> = emptyMap(),
    /** Category id to its stored icon key. */
    val categoryIcons: Map<Long, String> = emptyMap()
)

/** The word that says what kind of row this is, so colour is never the only signal. */
enum class TxnTag(val word: String) {
    SHARED("Shared"), OWES_YOU("Owes you"), YOU_OWE("You owe"), SETTLED("Settled")
}

/**
 * One row as the lists show it. A shared expense is one item: [txn] is the whole amount paid and
 * [shares] are the dues saved with it.
 */
data class TxnItem(
    val txn: TransactionEntity,
    val shares: List<TransactionEntity>,
    val title: String,
    val tag: TxnTag?,
    val detail: String,
    /** Paise, negative for money out or owed by you. */
    val signedAmount: Long,
    val people: List<String>,
    val category: String = "",
    val categoryIcon: String? = null
) {
    val yourShare: Long get() = txn.amount - shares.sumOf { it.amount }
}

object TxnItems {

    /**
     * Folds a split (one expense and its dues, written together and so sharing both timestamps) into
     * one item. Input order is kept.
     */
    fun of(transactions: List<TransactionEntity>, names: TxnNames): List<TxnItem> {
        val sharesByExpense = mutableMapOf<Long, List<TransactionEntity>>()
        transactions
            .filter { it.type == TxnType.EXPENSE || it.type == TxnType.DUE_TO }
            .groupBy { it.createdAt to it.transactedAt }
            .values
            .forEach { group ->
                val expense = group.singleOrNull { it.type == TxnType.EXPENSE } ?: return@forEach
                val dues = group.filter { it.type == TxnType.DUE_TO && it.payeeId != null }
                if (dues.isNotEmpty()) sharesByExpense[expense.id] = dues
            }
        val folded = sharesByExpense.values.flatten().map { it.id }.toSet()
        return transactions
            .filterNot { it.id in folded }
            .map { item(it, sharesByExpense[it.id].orEmpty(), names) }
    }

    private fun item(txn: TransactionEntity, shares: List<TransactionEntity>, names: TxnNames): TxnItem {
        val payee = txn.payeeId?.let { names.payees[it] }
        val category = names.categories[txn.categoryId] ?: "Uncategorized"
        val account = txn.accountId?.let { names.accounts[it] }
        val note = txn.note?.takeIf { it.isNotBlank() }
        val people = (shares.mapNotNull { names.payees[it.payeeId] } + listOfNotNull(payee)).distinct()
        val who = payee ?: "Someone"

        var title = note ?: payee ?: category
        var tag: TxnTag? = null
        var detail = listOf(category, account)
        var sign = -1
        when (txn.type) {
            TxnType.EXPENSE -> if (shares.isNotEmpty()) {
                title = note ?: category
                tag = TxnTag.SHARED
                val share = txn.amount - shares.sumOf { it.amount }
                // Who owes what, in words. Past two people the row only counts them.
                val owed = if (shares.size <= 2) {
                    shares.map { "${names.payees[it.payeeId] ?: "Someone"} owes ${MoneyFormatter.format(it.amount).text}" }
                } else {
                    listOf("${shares.size} people owe ${MoneyFormatter.format(shares.sumOf { it.amount }).text}")
                }
                detail = listOf(account, "your share ${MoneyFormatter.format(share).text}") + owed
            }
            TxnType.INCOME -> {
                detail = listOf("Income", account)
                sign = 1
            }
            TxnType.DUE_TO -> {
                title = note ?: who
                tag = TxnTag.OWES_YOU
                detail = listOf(who)
                sign = 1
            }
            TxnType.DUE_FROM -> {
                title = note ?: who
                tag = TxnTag.YOU_OWE
                detail = listOf(who)
            }
            TxnType.SETTLEMENT_FROM -> {
                title = "$who paid you"
                tag = TxnTag.SETTLED
                detail = listOf(account, note)
                sign = 1
            }
            TxnType.SETTLEMENT_TO -> {
                title = "You paid $who"
                tag = TxnTag.SETTLED
                detail = listOf(account, note)
            }
            TxnType.TRANSFER_IN, TxnType.TRANSFER_OUT -> {
                title = note ?: "Transfer"
                detail = listOf("Transfer", account)
                if (txn.type == TxnType.TRANSFER_IN) sign = 1
            }
        }
        return TxnItem(
            txn = txn,
            shares = shares,
            title = title,
            tag = tag,
            detail = detail.filterNotNull().joinToString(" · "),
            signedAmount = sign * txn.amount,
            people = people,
            category = category,
            categoryIcon = names.categoryIcons[txn.categoryId]
        )
    }
}

private val dayMonth = DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH)
private val dayMonthYear = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH)

/** "Today", "Yesterday", "19 Oct", with the year only once it differs. */
fun dayLabel(date: LocalDate, today: LocalDate): String = when {
    date == today -> "Today"
    date == today.minusDays(1) -> "Yesterday"
    date.year == today.year -> date.format(dayMonth)
    else -> date.format(dayMonthYear)
}
