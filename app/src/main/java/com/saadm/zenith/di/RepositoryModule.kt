package com.saadm.zenith.di

import android.content.Context
import com.saadm.zenith.data.preferences.AppPreferencesStore
import com.saadm.zenith.data.repository.BudgetRepoImpl
import com.saadm.zenith.data.repository.PayeeRepoImpl
import com.saadm.zenith.data.repository.TransactionRepoImpl
import com.saadm.zenith.domain.repository.BudgetRepo
import com.saadm.zenith.domain.repository.PayeeRepo
import com.saadm.zenith.domain.repository.TransactionRepo
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Binds repository implementations to their interfaces. */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

	@Binds
	@Singleton
	abstract fun bindPayeeRepo(impl: PayeeRepoImpl): PayeeRepo

	@Binds
	@Singleton
	abstract fun bindTransactionRepo(impl: TransactionRepoImpl): TransactionRepo

	@Binds
	@Singleton
	abstract fun bindBudgetRepo(impl: BudgetRepoImpl): BudgetRepo

	companion object {
		@Provides
		@Singleton
		fun provideAppPreferencesStore(@ApplicationContext context: Context): AppPreferencesStore =
			AppPreferencesStore(context)
	}
}
