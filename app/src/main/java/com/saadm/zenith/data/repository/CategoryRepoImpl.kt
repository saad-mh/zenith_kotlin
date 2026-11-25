package com.saadm.zenith.data.repository

import com.saadm.zenith.data.db.dao.CategoryDao
import com.saadm.zenith.data.entity.CategoryEntity
import com.saadm.zenith.data.entity.TxnType
import kotlinx.coroutines.flow.Flow
import com.saadm.zenith.domain.repository.CategoryRepo
import javax.inject.Inject

class CategoryRepoImpl @Inject constructor(
    private val dao: CategoryDao
) : CategoryRepo {
    override fun observeAll(): Flow<List<CategoryEntity>> = dao.observeEvery()

    override suspend fun defaultCategoryId(txnType: TxnType): Long? =
        dao.getDefaultByType(txnType.name)?.id
}
