package com.saadm.zenith.ui.add

import com.saadm.zenith.data.entity.CategoryEntity
import com.saadm.zenith.data.entity.TxnType
import org.junit.Assert.assertEquals
import org.junit.Test

class AddSheetTest {
    @Test
    fun draftHintShowsFiveTimesThenRarely() {
        assertEquals(listOf(0, 1, 2, 3, 4, 20, 40), (0..45).filter(::showDraftHint))
    }

    @Test
    fun unpickedCategoryFallsBackToTheDefault() {
        val food = CategoryEntity(1, "Food", "DINE", "#000000", TxnType.EXPENSE, 0)
        val other = food.copy(id = 2, name = "Uncategorized", isDefault = true)
        assertEquals(other, effectiveCategory(listOf(food, other), null))
        assertEquals(other, effectiveCategory(listOf(food, other), 99))
        assertEquals(food, effectiveCategory(listOf(food, other), 1))
    }
}
