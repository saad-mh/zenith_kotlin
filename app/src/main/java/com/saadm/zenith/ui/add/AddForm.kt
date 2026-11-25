package com.saadm.zenith.ui.add

import com.saadm.zenith.data.entity.TransactionEntity
import com.saadm.zenith.data.entity.TxnType
import com.saadm.zenith.domain.model.MoneyFormatter
import com.saadm.zenith.domain.model.SplitMode
import com.saadm.zenith.domain.split.Split
import com.saadm.zenith.domain.split.SplitItem
import com.saadm.zenith.domain.split.YOU
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

/** One line of an itemised bill as typed. */
data class DraftItem(val amount: String = "", val participants: Set<Long> = emptySet())

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
    val categoryPicked: Boolean = false,
    /** Payee ids sharing an expense with the user. */
    val people: List<Long> = emptyList(),
    val splitMode: SplitMode = SplitMode.EQUAL,
    val custom: Map<Long, String> = emptyMap(),
    val items: List<DraftItem> = emptyList(),
    val sendRequest: Boolean = false
) {
    /** What each of [people] owes, or null when the typed figures don't fit the total. */
    fun shares(): Map<Long, Long>? {
        val total = paise ?: return emptyMap()
        return when (splitMode) {
            SplitMode.EQUAL -> Split.equal(total, people)
            SplitMode.CUSTOM -> Split.custom(total, people.associateWith { MoneyFormatter.parse(custom[it].orEmpty()) ?: 0L })
            SplitMode.BY_ITEMS -> Split.byItems(
                total,
                people,
                items.mapNotNull { item ->
                    MoneyFormatter.parse(item.amount)?.let { amount ->
                        SplitItem(amount, item.participants.filter { it == YOU || it in people }.toSet())
                    }
                }
            )
        }
    }

    val kind: AddKind get() = AddKind.of(type)

    /** The typed sum in paise, or null while it can't be saved. */
    val paise: Long? get() = evaluate(amount)

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
            paise == null -> amountProblem(amount)
            needsPayee && payeeId == null -> "Choose a person"
            else -> null
        }
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

/**
 * Drops ids that no longer exist, so a draft or a saved default that outlived a restore, delete-all or
 * archive can't break a save. [accounts] and [payees] are the ids that may still be used.
 */
fun AddForm.withKnownIds(accounts: Set<Long>, payees: Set<Long>): AddForm = copy(
    accountId = accountId?.takeIf { it in accounts },
    payeeId = payeeId?.takeIf { it in payees },
    people = people.filter { it in payees }
)

/** Holds the unsaved entry while the sheet is closed. */
// shortcut: memory only, so a draft is lost if the process dies; move to DataStore if that matters.
@Singleton
class AddDraft @Inject constructor() {
    var form: AddForm? = null
}
