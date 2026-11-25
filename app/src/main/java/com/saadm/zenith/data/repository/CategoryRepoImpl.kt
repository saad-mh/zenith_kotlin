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

    // Update rather than upsert for an existing row, so nothing referencing it is touched.
    override suspend fun save(category: CategoryEntity): Long =
        if (category.id == 0L) dao.upsert(category) else category.id.also { dao.update(category) }

    override suspend fun archive(id: Long) = dao.archive(id)

    override suspend fun merge(from: Long, into: Long) = dao.merge(from, into)

    override suspend fun defaultCategoryId(txnType: TxnType): Long? =
        dao.getDefaultByType(txnType.name)?.id
}
