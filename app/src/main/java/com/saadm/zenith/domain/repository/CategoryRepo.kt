package com.saadm.zenith.domain.repository

import com.saadm.zenith.data.entity.CategoryEntity
import com.saadm.zenith.data.entity.TxnType
import kotlinx.coroutines.flow.Flow

interface CategoryRepo {
    /** Every category, archived ones included, so old entries still show their name. */
    fun observeAll(): Flow<List<CategoryEntity>>

    /** The default category for a kind (INCOME or EXPENSE), used where a screen does not pick one. */
    suspend fun defaultCategoryId(txnType: TxnType): Long?
}
