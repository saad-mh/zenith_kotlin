package com.saadm.zenith.data.entity

enum class TxnType {
    INCOME,
    EXPENSE,
    DUE_TO,
    DUE_FROM,
    SETTLEMENT_FROM,
    SETTLEMENT_TO,
    TRANSFER_IN,
    TRANSFER_OUT
}

enum class SettlementStatus {
    PENDING,
    PARTIAL,
    SETTLED
}

enum class AccountType {
    CASH,
    UPI,
    DEBIT_CARD,
    CREDIT_CARD,
    BANK_TRANSFER
}

enum class BudgetPeriod {
    MONTHLY,
    WEEKLY,
    DAILY,
    CUSTOM
}
