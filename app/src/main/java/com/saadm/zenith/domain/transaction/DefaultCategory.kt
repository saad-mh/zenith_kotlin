package com.saadm.zenith.domain.transaction

import com.saadm.zenith.data.entity.CategoryEntity
import com.saadm.zenith.data.entity.TransactionEntity
import com.saadm.zenith.data.entity.TxnType

/** Stored in place of a category id: start new entries with the one chosen last. */
const val LAST_USED_CATEGORY = -1L

// Settlements and transfers are filed automatically, so they say nothing about what was chosen.
private val chosenByHand = setOf(TxnType.EXPENSE, TxnType.INCOME, TxnType.DUE_TO, TxnType.DUE_FROM)

/**
 * The category a new entry starts with. [categories] are the usable ones of its kind; [preference] is
 * a category id, [LAST_USED_CATEGORY] or null. Anything that no longer applies gives the built-in default.
 */
fun preferredCategory(preference: Long?, categories: List<CategoryEntity>, history: List<TransactionEntity>): Long? {
    val usable = categories.map { it.id }.toSet()
    val wanted = if (preference == LAST_USED_CATEGORY) {
        history.filter { it.type in chosenByHand && it.categoryId in usable }.maxByOrNull { it.createdAt }?.categoryId
    } else {
        preference?.takeIf { it in usable }
    }
    return wanted ?: categories.firstOrNull { it.isDefault }?.id
}
