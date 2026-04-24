package com.saadm.zenith.data.db.converter

import androidx.room.TypeConverter
import com.saadm.zenith.data.entity.AccountType
import com.saadm.zenith.data.entity.BudgetPeriod
import com.saadm.zenith.data.entity.TxnType

class DbTypeConverters {
    @TypeConverter
    fun fromTxnType(value: TxnType?): String? = value?.name

    @TypeConverter
    fun toTxnType(value: String?): TxnType? = value?.let(TxnType::valueOf)

    @TypeConverter
    fun fromAccountType(value: AccountType?): String? = value?.name

    @TypeConverter
    fun toAccountType(value: String?): AccountType? = value?.let(AccountType::valueOf)

    @TypeConverter
    fun fromBudgetPeriod(value: BudgetPeriod?): String? = value?.name

    @TypeConverter
    fun toBudgetPeriod(value: String?): BudgetPeriod? = value?.let(BudgetPeriod::valueOf)
}

