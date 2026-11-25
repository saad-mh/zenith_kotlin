package com.saadm.zenith.data.db

import androidx.room.Room
import com.saadm.zenith.data.entity.AccountEntity
import com.saadm.zenith.data.entity.AccountType
import com.saadm.zenith.data.entity.PayeeEntity
import com.saadm.zenith.data.entity.TransactionEntity
import com.saadm.zenith.data.entity.TxnType
import com.saadm.zenith.data.repository.AccountRepoImpl
import com.saadm.zenith.data.repository.CategoryRepoImpl
import com.saadm.zenith.data.repository.PayeeRepoImpl
import com.saadm.zenith.data.repository.TransactionRepoImpl
import com.saadm.zenith.domain.usecase.ArchiveAccount
import com.saadm.zenith.domain.usecase.ObserveNetPosition
import com.saadm.zenith.domain.usecase.RecordTransfer
import com.saadm.zenith.domain.usecase.SaveAccount
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/** Balances, transfers and net position, run against a real in-memory database. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AccountsRoomTest {

    private lateinit var db: AppDatabase
    private lateinit var accounts: AccountRepoImpl
    private lateinit var transactions: TransactionRepoImpl
    private lateinit var payees: PayeeRepoImpl
    private lateinit var transfer: RecordTransfer
    private val food = 3L
    private var cash = 0L
    private var upi = 0L

    @Before
    fun setUp() = runBlocking {
        db = Room.inMemoryDatabaseBuilder(RuntimeEnvironment.getApplication(), AppDatabase::class.java)
            .allowMainThreadQueries()
            .addCallback(AppDatabase.DB_CREATE_CALLBACK)
            .build()
        accounts = AccountRepoImpl(db.accountDao())
        transactions = TransactionRepoImpl(db.transactionDao())
        payees = PayeeRepoImpl(db.payeeDao(), db.payeeBalanceDao(), db.transactionDao())
        transfer = RecordTransfer(transactions, CategoryRepoImpl(db.categoryDao()))
        cash = SaveAccount(accounts)(AccountEntity(name = "Cash", type = AccountType.CASH, colorHex = "#000000", emoji = "C", sortOrder = 0, openingBalance = 100_000))
        upi = SaveAccount(accounts)(AccountEntity(name = "UPI", type = AccountType.UPI, colorHex = "#000000", emoji = "U", sortOrder = 1))
    }

    @After
    fun tearDown() {
        db.close()
    }

    private suspend fun add(type: TxnType, amount: Long, accountId: Long?, payeeId: Long? = null, deleted: Boolean = false) {
        transactions.save(
            TransactionEntity(
                amount = amount, type = type, transactedAt = 1_000, createdAt = 1_000, updatedAt = 1_000,
                categoryId = food, payeeId = payeeId, accountId = accountId, note = null, receiptUri = null,
                isDeleted = deleted
            )
        )
    }

    private suspend fun balanceOf(id: Long) = accounts.observeBalances().first().first { it.id == id }.balance

    @Test
    fun balancesAreOpeningPlusCashMovementsOnly() = runBlocking {
        add(TxnType.INCOME, 50_000, accountId = cash)
        add(TxnType.EXPENSE, 20_000, accountId = cash)
        add(TxnType.DUE_TO, 9_999, accountId = cash)            // dues move no money
        add(TxnType.EXPENSE, 7_777, accountId = cash, deleted = true)
        move(cash, upi, amount = 30_000, at = 2_000)
        add(TxnType.SETTLEMENT_FROM, 10_000, accountId = upi)

        assertEquals(100_000L, balanceOf(cash))   // 100,000 + 50,000 - 20,000 - 30,000
        assertEquals(40_000L, balanceOf(upi))     // 30,000 transferred in + 10,000 settlement
    }

    @Test
    fun transfersAreNotIncomeOrSpending() = runBlocking {
        move(cash, upi, amount = 30_000, at = 2_000)
        val summary = com.saadm.zenith.domain.usecase.ObserveMonthSummary(transactions)(
            com.saadm.zenith.domain.model.MonthPeriods.containing(java.time.Instant.ofEpochMilli(2_000).atZone(java.time.ZoneId.systemDefault()).toLocalDate())
        ).first()
        assertEquals(0L, summary.income)
        assertEquals(0L, summary.expense)
    }

    @Test
    fun editingAnAccountKeepsItsTransactions() = runBlocking {
        add(TxnType.EXPENSE, 20_000, accountId = cash)
        SaveAccount(accounts)(AccountEntity(id = cash, name = "Wallet", type = AccountType.CASH, colorHex = "#000000", emoji = "C", sortOrder = 0, openingBalance = 100_000))

        // An edit must not null out the account on the transactions that use it.
        assertEquals(80_000L, balanceOf(cash))
    }

    @Test
    fun archivedAccountsDropOutOfBalancesAndNetPosition() = runBlocking {
        ArchiveAccount(accounts)(upi)

        assertEquals(listOf(cash), accounts.observeBalances().first().map { it.id })
        val position = ObserveNetPosition(accounts, payees, transactions)().first()
        assertEquals(100_000L, position.accountsTotal)
        assertEquals(1, position.accountCount)
    }

    @Test
    fun transfersNeedTwoDifferentAccountsAndAPositiveAmount() = runBlocking<Unit> {
        assertThrows(IllegalArgumentException::class.java) { runBlocking { transfer(cash, cash, 100, at = 1) } }
        assertThrows(IllegalArgumentException::class.java) { runBlocking { transfer(cash, upi, 0, at = 1) } }
    }

    @Test
    fun netPositionAddsFriendsToAccounts() = runBlocking {
        val asha = db.payeeDao().upsert(PayeeEntity(name = "Asha", avatarUri = null, phone = null, upiId = null, createdAt = 0))
        add(TxnType.DUE_TO, 5_000, accountId = null, payeeId = asha)
        add(TxnType.EXPENSE, 20_000, accountId = cash)

        val position = ObserveNetPosition(accounts, payees, transactions)().first()

        assertEquals(80_000L, position.accountsTotal)
        assertEquals(5_000L, position.friendsNet)
        assertEquals(85_000L, position.net)
        assertEquals("₹800 in 2 accounts + ₹50 from friends", position.subtitle())
    }

    private suspend fun move(from: Long, to: Long, amount: Long, at: Long) {
        transfer(from, to, amount, at, note = null, now = at)
    }
}
