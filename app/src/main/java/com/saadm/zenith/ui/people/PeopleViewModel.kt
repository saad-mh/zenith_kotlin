package com.saadm.zenith.ui.people

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.saadm.zenith.data.entity.PayeeEntity
import com.saadm.zenith.domain.model.Payee
import com.saadm.zenith.domain.repository.PayeeRepo
import kotlinx.coroutines.flow.Flow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for managing the People (Payees) feature.
 * Handles CRUD operations and exposition of payee data with derived summaries.
 *
 * Note: Currently not directly used by PeopleManagementContent which accesses DAOs directly,
 * but available for future extended use cases that need more complex state management.
 */
@HiltViewModel
class PeopleViewModel @Inject constructor(
	private val payeeRepo: PayeeRepo
) : ViewModel() {

	/** All active payees with derived financial summaries. */
	val payees: StateFlow<List<Payee>> = payeeRepo.observeAllActive()
		.stateIn(
			scope = viewModelScope,
			started = SharingStarted.WhileSubscribed(5000),
			initialValue = emptyList()
		)

	/** Observe one payee with live transaction history for details screens. */
	fun observePayeeById(id: Long): Flow<Payee?> = payeeRepo.observeById(id)

	/**
	 * Add or update a payee.
	 */
	fun upsertPayee(payee: PayeeEntity) {
		viewModelScope.launch {
			payeeRepo.upsert(payee)
		}
	}

	/**
	 * Update a payee.
	 */
	fun updatePayee(payee: PayeeEntity) {
		viewModelScope.launch {
			payeeRepo.update(payee)
		}
	}

	/**
	 * Soft-delete a payee.
	 */
	fun deletePayee(id: Long) {
		viewModelScope.launch {
			payeeRepo.softDelete(id)
		}
	}
}


