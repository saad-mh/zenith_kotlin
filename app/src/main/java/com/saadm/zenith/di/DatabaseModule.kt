package com.saadm.zenith.di

import android.content.Context
import com.saadm.zenith.data.db.AppDatabase
import com.saadm.zenith.data.db.DatabaseProvider
import com.saadm.zenith.data.db.dao.AccountDao
import com.saadm.zenith.data.db.dao.BudgetDao
import com.saadm.zenith.data.db.dao.CategoryDao
import com.saadm.zenith.data.db.dao.PayeeBalanceDao
import com.saadm.zenith.data.db.dao.PayeeDao
import com.saadm.zenith.data.db.dao.TransactionDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

	@Provides
	@Singleton
	fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
		return DatabaseProvider.getInstance(context)
	}

	@Provides
	@Singleton
	fun provideTransactionDao(database: AppDatabase): TransactionDao = database.transactionDao()

	@Provides
	@Singleton
	fun providePayeeDao(database: AppDatabase): PayeeDao = database.payeeDao()

	@Provides
	@Singleton
	fun providePayeeBalanceDao(database: AppDatabase): PayeeBalanceDao = database.payeeBalanceDao()

	@Provides
	@Singleton
	fun provideCategoryDao(database: AppDatabase): CategoryDao = database.categoryDao()

	@Provides
	@Singleton
	fun provideAccountDao(database: AppDatabase): AccountDao = database.accountDao()

	@Provides
	@Singleton
	fun provideBudgetDao(database: AppDatabase): BudgetDao = database.budgetDao()
}

