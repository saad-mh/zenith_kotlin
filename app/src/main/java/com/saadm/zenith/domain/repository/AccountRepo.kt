package com.saadm.zenith.domain.repository

import com.saadm.zenith.data.entity.AccountBalance
import com.saadm.zenith.data.entity.AccountEntity
import kotlinx.coroutines.flow.Flow

/** Accounts and payment methods, with balances derived from their transactions. */
interface AccountRepo {

    /** Active accounts in display order, each with its balance. */
    fun observeBalances(): Flow<List<AccountBalance>>

    /** Adds an account (id 0) or updates an existing one. Returns the id. */
    suspend fun save(account: AccountEntity): Long

    /** Hides an account. Its transactions stay, and it drops out of balances and pickers. */
    suspend fun archive(id: Long)
}
