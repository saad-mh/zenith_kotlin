package com.saadm.zenith.domain.repository

import com.saadm.zenith.data.entity.CategoryEntity
import com.saadm.zenith.data.entity.TxnType
import kotlinx.coroutines.flow.Flow

interface CategoryRepo {
    /** Every category, archived ones included, so old entries still show their name. */
    fun observeAll(): Flow<List<CategoryEntity>>

    /** Adds a category (id 0) or updates an existing one. Returns the id. */
    suspend fun save(category: CategoryEntity): Long

    /** Hides a category. Its transactions keep it. */
    suspend fun archive(id: Long)

    /** Moves every transaction and budget from [from] to [into], then archives [from], in one write. */
    suspend fun merge(from: Long, into: Long)

    /** The default category for a kind (INCOME or EXPENSE), used where a screen does not pick one. */
    suspend fun defaultCategoryId(txnType: TxnType): Long?
}
