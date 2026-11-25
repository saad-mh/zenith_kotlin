package com.saadm.zenith.domain.repository

import com.saadm.zenith.data.entity.TxnType

interface CategoryRepo {
    /** The default category for a kind (INCOME or EXPENSE), used where a screen does not pick one. */
    suspend fun defaultCategoryId(txnType: TxnType): Long?
}
