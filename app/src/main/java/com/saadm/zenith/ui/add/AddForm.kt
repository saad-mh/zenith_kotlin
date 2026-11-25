package com.saadm.zenith.ui.add

import com.saadm.zenith.data.entity.TransactionEntity
import com.saadm.zenith.data.entity.TxnType
import com.saadm.zenith.domain.model.MoneyFormatter
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/** The four tabs. Two of them have a direction toggle: the first type is the default. */
enum class AddKind(val label: String, val types: List<TxnType>) {
    Expense("Expense", listOf(TxnType.EXPENSE)),
    Income("Income", listOf(TxnType.INCOME)),
    Owed("Owed", listOf(TxnType.DUE_TO, TxnType.DUE_FROM)),
    Paid("You paid", listOf(TxnType.SETTLEMENT_TO, TxnType.SETTLEMENT_FROM));

    companion object {
        fun of(type: TxnType): AddKind = entries.firstOrNull { type in it.types } ?: Expense
    }
}

/** Everything typed so far. [text] is the merchant or note; the table has one column for both. */
data class AddForm(
    val date: LocalDate,
    val type: TxnType = TxnType.EXPENSE,
    val amount: String = "",
    val categoryId: Long? = null,
    val accountId: Long? = null,
    val payeeId: Long? = null,
    val text: String = "",
    /** True once the user picks a category, so a suggestion never overrides the choice. */
    val categoryPicked: Boolean = false
) {
    val kind: AddKind get() = AddKind.of(type)

    val paise: Long? get() = MoneyFormatter.parse(amount)

    /** Dues and settlements are between you and one person. */
    val needsPayee: Boolean get() = kind == AddKind.Owed || kind == AddKind.Paid

    /** Which categories apply, or null where the entry is not filed under a chosen one. */
    val categoryType: TxnType?
        get() = when (type) {
            TxnType.INCOME -> TxnType.INCOME
            TxnType.EXPENSE, TxnType.DUE_TO, TxnType.DUE_FROM -> TxnType.EXPENSE
            else -> null
        }

    /** What still stops a save, or null when it can go ahead. */
    val problem: String?
        get() = when {
            paise == null -> "Enter an amount"
            needsPayee && payeeId == null -> "Choose a person"
            else -> null
        }
}

private const val MAX_WHOLE_DIGITS = 9
const val BACKSPACE = "⌫"

/** The amount after one keypad press. Presses that would make an unreadable amount are ignored. */
fun typeKey(current: String, key: String): String {
    if (key == BACKSPACE) return current.dropLast(1)
    if (key == ".") return if ('.' in current) current else current.ifEmpty { "0" } + "."
    val next = if (current == "0") key else current + key
    val whole = next.substringBefore('.')
    val decimals = next.substringAfter('.', "")
    return if (whole.length > MAX_WHOLE_DIGITS || decimals.length > 2) current else next
}

/** The typed amount with Indian grouping, keeping a trailing point or zero as typed. */
fun displayAmount(amount: String): String {
    val whole = amount.substringBefore('.').ifEmpty { "0" }
    val grouped = MoneyFormatter.format(whole.toLong() * 100).whole
    return if ('.' in amount) "$grouped.${amount.substringAfter('.')}" else grouped
}

/** The category last used for this merchant or note, if there is one. */
fun suggestCategory(text: String, history: List<TransactionEntity>, allowed: Set<Long>): Long? {
    val wanted = text.trim()
    if (wanted.length < 3) return null
    return history
        .filter { it.categoryId in allowed && it.note?.startsWith(wanted, ignoreCase = true) == true }
        .maxByOrNull { it.transactedAt }
        ?.categoryId
}

/** Holds the unsaved entry while the sheet is closed. */
// shortcut: memory only, so a draft is lost if the process dies; move to DataStore if that matters.
@Singleton
class AddDraft @Inject constructor() {
    var form: AddForm? = null
}
