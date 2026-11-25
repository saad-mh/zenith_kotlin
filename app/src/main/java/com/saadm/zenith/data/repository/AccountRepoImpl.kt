package com.saadm.zenith.data.repository

import com.saadm.zenith.data.db.dao.AccountDao
import com.saadm.zenith.data.entity.AccountBalance
import com.saadm.zenith.data.entity.AccountEntity
import com.saadm.zenith.domain.repository.AccountRepo
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class AccountRepoImpl @Inject constructor(
    private val dao: AccountDao
) : AccountRepo {

    override fun observeBalances(): Flow<List<AccountBalance>> = dao.observeBalances()

    override suspend fun save(account: AccountEntity): Long =
        if (account.id == 0L) dao.insert(account) else account.id.also { dao.update(account) }

    override suspend fun archive(id: Long) = dao.archive(id)
}
