package com.saadm.zenith.domain.usecase

import com.saadm.zenith.domain.repository.AccountRepo
import com.saadm.zenith.domain.repository.CategoryRepo
import com.saadm.zenith.domain.repository.PayeeRepo
import com.saadm.zenith.domain.transaction.TxnNames
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

class ObserveTxnNames @Inject constructor(
    private val categories: CategoryRepo,
    private val payees: PayeeRepo,
    private val accounts: AccountRepo
) {
    operator fun invoke(): Flow<TxnNames> =
        combine(categories.observeAll(), payees.observeAllActive(), accounts.observeBalances()) { c, p, a ->
            TxnNames(
                categories = c.associate { it.id to it.name },
                payees = p.associate { it.id to it.name },
                accounts = a.associate { it.id to it.name }
            )
        }
}
