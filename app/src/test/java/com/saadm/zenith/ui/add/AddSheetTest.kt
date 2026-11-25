package com.saadm.zenith.ui.add

import com.saadm.zenith.data.entity.CategoryEntity
import com.saadm.zenith.data.entity.TransactionEntity
import com.saadm.zenith.data.entity.TxnType
import com.saadm.zenith.domain.transaction.LAST_USED_CATEGORY
import com.saadm.zenith.domain.transaction.preferredCategory
import org.junit.Assert.assertEquals
import org.junit.Test

class AddSheetTest {
    @Test
    fun draftHintShowsFiveTimesThenRarely() {
        assertEquals(listOf(0, 1, 2, 3, 4, 20, 40), (0..45).filter(::showDraftHint))
    }

    private val food = CategoryEntity(1, "Food", "DINE", "#000000", TxnType.EXPENSE, 0)
    private val other = food.copy(id = 2, name = "Uncategorized", isDefault = true)
    private val both = listOf(food, other)

    private fun entry(categoryId: Long, createdAt: Long, type: TxnType = TxnType.EXPENSE) = TransactionEntity(
        amount = 100, type = type, transactedAt = 0, createdAt = createdAt, updatedAt = 0,
        categoryId = categoryId, payeeId = null, accountId = null, note = null, receiptUri = null
    )

    @Test
    fun unpickedCategoryShowsTheDefault() {
        assertEquals(other, effectiveCategory(both, null, 2))
        assertEquals(food, effectiveCategory(both, 1, 2))
    }

    @Test
    fun preferredCategoryFollowsTheSettingAndFallsBackToTheBuiltInOne() {
        assertEquals(2L, preferredCategory(null, both, emptyList()))
        assertEquals(1L, preferredCategory(1, both, emptyList()))
        // Archived or merged away since it was chosen.
        assertEquals(2L, preferredCategory(9, both, emptyList()))
    }

    @Test
    fun lastUsedTakesTheNewestHandPickedEntry() {
        val history = listOf(entry(2, createdAt = 1), entry(1, createdAt = 5), entry(2, createdAt = 9, TxnType.SETTLEMENT_TO))
        assertEquals(1L, preferredCategory(LAST_USED_CATEGORY, both, history))
        assertEquals(2L, preferredCategory(LAST_USED_CATEGORY, both, emptyList()))
    }
}
