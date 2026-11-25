package com.saadm.zenith.di

import android.content.Context
import com.saadm.zenith.data.preferences.AppPreferencesStore
import dagger.hilt.android.qualifiers.ApplicationContext
import com.saadm.zenith.data.db.dao.PayeeBalanceDao
import com.saadm.zenith.data.db.dao.PayeeDao
import com.saadm.zenith.data.db.dao.TransactionDao
import com.saadm.zenith.data.repository.PayeeRepoImpl
import com.saadm.zenith.domain.repository.PayeeRepo
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt DI module for binding repository implementations to their interfaces.
 */
@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

	@Provides
	@Singleton
	fun provideAppPreferencesStore(@ApplicationContext context: Context): AppPreferencesStore =
		AppPreferencesStore(context)

	@Provides
	@Singleton
	fun providePayeeRepo(
		payeeDao: PayeeDao,
		payeeBalanceDao: PayeeBalanceDao,
		transactionDao: TransactionDao
	): PayeeRepo = PayeeRepoImpl(payeeDao, payeeBalanceDao, transactionDao)
}

