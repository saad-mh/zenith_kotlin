package com.saadm.zenith.domain.repository

import com.saadm.zenith.data.db.dao.PayeeBalanceDao
import com.saadm.zenith.data.db.dao.PayeeDao
import com.saadm.zenith.data.db.dao.TransactionDao
import com.saadm.zenith.data.entity.PayeeBalanceView
import com.saadm.zenith.data.entity.TransactionEntity
import com.saadm.zenith.data.entity.TxnType
import com.saadm.zenith.data.repository.PayeeRepoImpl
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations

/**
 * Test suite for PayeeRepo implementation.
 * Verifies that derived summaries are correctly computed from transactions.
 */
class PayeeRepoTest {

	@Mock
	private lateinit var payeeDao: PayeeDao

	@Mock
	private lateinit var payeeBalanceDao: PayeeBalanceDao

	@Mock
	private lateinit var transactionDao: TransactionDao

	private lateinit var repo: PayeeRepo

	@Before
	fun setUp() {
		MockitoAnnotations.openMocks(this)
		repo = PayeeRepoImpl(payeeDao, payeeBalanceDao, transactionDao)
	}

	@Test
	fun testObserveAllActive() = runBlocking {
		// Given: Two payees with different balances
		val balance1 = PayeeBalanceView(
			id = 1L,
			name = "Alice",
			avatarUri = null,
			phone = "+91 9999999999",
			upiId = null,
			createdAt = System.currentTimeMillis(),
			isDeleted = false,
			dueToAmount = 5000,  // Alice owes 50 (in smallest unit)
			dueFromAmount = 0,
			transactionCount = 2,
			lastInteractionAt = System.currentTimeMillis()
		)
		val balance2 = PayeeBalanceView(
			id = 2L,
			name = "Bob",
			avatarUri = null,
			phone = null,
			upiId = "bob@upi",
			createdAt = System.currentTimeMillis(),
			isDeleted = false,
			dueToAmount = 0,
			dueFromAmount = 3000,    // User owes Bob 30
			transactionCount = 1,
			lastInteractionAt = System.currentTimeMillis()
		)

		`when`(payeeBalanceDao.observeAllActive())
			.thenReturn(flowOf(listOf(balance1, balance2)))

		// When: Fetching all payees
		val payees = mutableListOf<List<com.saadm.zenith.domain.model.Payee>>()
		repo.observeAllActive().collect { payees.add(it) }

		// Then: Both payees returned with correct summaries
		assertEquals(1, payees.size)
		assertEquals(2, payees[0].size)

		val alice = payees[0].find { it.id == 1L }
		assertNotNull(alice)
		assertEquals("Alice", alice!!.name)
		assertEquals(5000, alice.dueToAmount)
		assertEquals(0, alice.dueFromAmount)
		assertEquals(5000, alice.netBalance)
		assertEquals(2, alice.transactionCount)

		val bob = payees[0].find { it.id == 2L }
		assertNotNull(bob)
		assertEquals("Bob", bob!!.name)
		assertEquals(0, bob.dueToAmount)
		assertEquals(3000, bob.dueFromAmount)
		assertEquals(-3000, bob.netBalance)
		assertEquals(1, bob.transactionCount)
	}

	@Test
	fun testPayeeWithSettledBalance() {
		// Given: A payee with zero balance (settled)
		val balance = PayeeBalanceView(
			id = 3L,
			name = "Charlie",
			avatarUri = null,
			phone = null,
			upiId = null,
			createdAt = System.currentTimeMillis(),
			isDeleted = false,
			dueToAmount = 0,
			dueFromAmount = 0,
			transactionCount = 5,
			lastInteractionAt = System.currentTimeMillis()
		)

		// When: Converting to domain model
		val payee = com.saadm.zenith.domain.model.Payee(
			id = balance.id,
			name = balance.name,
			avatarUri = balance.avatarUri,
			phone = balance.phone,
			upiId = balance.upiId,
			createdAt = balance.createdAt,
			dueToAmount = balance.dueToAmount,
			dueFromAmount = balance.dueFromAmount,
			netBalance = balance.netBalanceAmount(),
			transactionCount = balance.transactionCount,
			lastInteractionAt = balance.lastInteractionAt
		)

		// Then: Balance is settled
		assertFalse(payee.hasActiveBalance())
		assertEquals("settled", payee.balanceText())
	}

	@Test
	fun testObserveByIdIncludesTransactions() = runBlocking {
		val payeeId = 1L
		val balance = PayeeBalanceView(
			id = payeeId,
			name = "Alice",
			avatarUri = null,
			phone = null,
			upiId = null,
			createdAt = System.currentTimeMillis(),
			isDeleted = false,
			dueToAmount = 1000,
			dueFromAmount = 500,
			transactionCount = 2,
			lastInteractionAt = System.currentTimeMillis()
		)
		val txns = listOf(
			TransactionEntity(
				id = 11L,
				amount = 1000,
				type = TxnType.DUE_TO,
				transactedAt = 200L,
				createdAt = 200L,
				updatedAt = 200L,
				categoryId = 1L,
				payeeId = payeeId,
				accountId = null,
				note = "Lunch",
				receiptUri = null
			),
			TransactionEntity(
				id = 12L,
				amount = 500,
				type = TxnType.DUE_FROM,
				transactedAt = 300L,
				createdAt = 300L,
				updatedAt = 300L,
				categoryId = 1L,
				payeeId = payeeId,
				accountId = null,
				note = "Tea",
				receiptUri = null
			)
		)

		`when`(payeeBalanceDao.observeById(payeeId)).thenReturn(flowOf(balance))
		`when`(transactionDao.observeActiveByPayeeId(payeeId)).thenReturn(flowOf(txns))

		val emissions = mutableListOf<com.saadm.zenith.domain.model.Payee?>()
		repo.observeById(payeeId).collect { emissions.add(it) }

		assertEquals(1, emissions.size)
		assertNotNull(emissions[0])
		assertEquals(2, emissions[0]!!.transactions.size)
	}
}

