package com.saadm.zenith.domain.model

import com.saadm.zenith.data.entity.TransactionEntity
import com.saadm.zenith.data.entity.TxnType

/** Which kinds of transaction a list shows. */
enum class TxnFilter(private val types: Set<TxnType>?) {
    ALL(null),
    INCOME(setOf(TxnType.INCOME)),
    EXPENSE(setOf(TxnType.EXPENSE)),
    OWES_YOU(setOf(TxnType.DUE_TO)),
    YOU_OWE(setOf(TxnType.DUE_FROM)),
    SETTLEMENTS(setOf(TxnType.SETTLEMENT_FROM, TxnType.SETTLEMENT_TO));

    fun matches(transaction: TransactionEntity): Boolean = types == null || transaction.type in types
}
