package com.saadm.zenith.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.saadm.zenith.data.db.converter.DbTypeConverters
import com.saadm.zenith.data.db.dao.AccountDao
import com.saadm.zenith.data.db.dao.BudgetDao
import com.saadm.zenith.data.db.dao.CategoryDao
import com.saadm.zenith.data.db.dao.PayeeDao
import com.saadm.zenith.data.db.dao.TransactionDao
import com.saadm.zenith.data.entity.AccountEntity
import com.saadm.zenith.data.entity.BudgetEntity
import com.saadm.zenith.data.entity.CategoryEntity
import com.saadm.zenith.data.entity.PayeeEntity
import com.saadm.zenith.data.entity.TransactionEntity

@Database(
	entities = [
		TransactionEntity::class,
		PayeeEntity::class,
		CategoryEntity::class,
		AccountEntity::class,
		BudgetEntity::class
	],
	version = 1,
	exportSchema = false
)
@TypeConverters(DbTypeConverters::class)
abstract class AppDatabase : RoomDatabase() {
	abstract fun transactionDao(): TransactionDao
	abstract fun payeeDao(): PayeeDao
	abstract fun categoryDao(): CategoryDao
	abstract fun accountDao(): AccountDao
	abstract fun budgetDao(): BudgetDao
}
