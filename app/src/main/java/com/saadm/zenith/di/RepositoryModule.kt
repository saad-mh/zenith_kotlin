package com.saadm.zenith.di

import com.saadm.zenith.data.db.dao.PayeeBalanceDao
import com.saadm.zenith.data.db.dao.PayeeDao
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
	fun providePayeeRepo(
		payeeDao: PayeeDao,
		payeeBalanceDao: PayeeBalanceDao
	): PayeeRepo = PayeeRepoImpl(payeeDao, payeeBalanceDao)
}

