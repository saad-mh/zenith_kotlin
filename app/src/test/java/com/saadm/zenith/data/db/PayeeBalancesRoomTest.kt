package com.saadm.zenith.data.db

import androidx.room.Room
import com.saadm.zenith.data.entity.PayeeEntity
import com.saadm.zenith.data.entity.TransactionEntity
import com.saadm.zenith.data.entity.TxnType
import com.saadm.zenith.data.repository.CategoryRepoImpl
import com.saadm.zenith.data.repository.TransactionRepoImpl
import com.saadm.zenith.domain.payee.BalanceDirection
import com.saadm.zenith.domain.usecase.ObservePayeeLedger
import com.saadm.zenith.domain.usecase.ObserveSharedBalances
import com.saadm.zenith.domain.usecase.RecordSettlement
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/** The payee_balances view and the settlement flow, run against a real in-memory database. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PayeeBalancesRoomTest {

    private lateinit var db: AppDatabase
    private lateinit var transactions: TransactionRepoImpl
    private lateinit var settle: RecordSettlement
    private val food = 3L
    private var payeeId = 0L

    @Before
    fun setUp() = runBlocking {
        db = Room.inMemoryDatabaseBuilder(RuntimeEnvironment.getApplication(), AppDatabase::class.java)
            .allowMainThreadQueries()
            .addCallback(AppDatabase.DB_CREATE_CALLBACK)
            .build()
        transactions = TransactionRepoImpl(db.transactionDao())
        settle = RecordSettlement(transactions, CategoryRepoImpl(db.categoryDao()))
        payeeId = db.payeeDao().upsert(PayeeEntity(name = "Asha", avatarUri = null, phone = null, upiId = null, createdAt = 0))
    }

    @After
    fun tearDown() {
        db.close()
    }

    private suspend fun due(type: TxnType, amount: Long, at: Long) {
        transactions.save(
            TransactionEntity(
                amount = amount, type = type, transactedAt = at, createdAt = at, updatedAt = at,
                categoryId = food, payeeId = payeeId, accountId = null, note = null, receiptUri = null
            )
        )
    }

    private fun viewRow() = db.payeeBalanceDao()

    @Test
    fun viewNetsSettlementsAgainstDues() = runBlocking {
        due(TxnType.DUE_TO, 10_000, at = 100)
        due(TxnType.SETTLEMENT_FROM, 4_000, at = 200)

        val row = viewRow().getById(payeeId)!!

        assertEquals(6_000L, row.dueToAmount)
        assertEquals(0L, row.dueFromAmount)
        assertEquals(6_000L, row.netBalanceAmount())
        assertEquals(2, row.transactionCount)
    }

    @Test
    fun overpaymentShowsTheOtherSide() = runBlocking {
        due(TxnType.DUE_TO, 5_000, at = 100)
        settle(payeeId, theyPaidYou = true, amount = 8_000, transactedAt = 200)

        val row = viewRow().getById(payeeId)!!

        assertEquals(0L, row.dueToAmount)
        assertEquals(3_000L, row.dueFromAmount)
        assertEquals(-3_000L, row.netBalanceAmount())
    }

    @Test
    fun youPayingThemReducesWhatYouOwe() = runBlocking {
        due(TxnType.DUE_FROM, 2_000, at = 100)
        settle(payeeId, theyPaidYou = false, amount = 500, transactedAt = 200)

        val row = viewRow().getById(payeeId)!!

        assertEquals(1_500L, row.dueFromAmount)
        assertEquals(0L, row.dueToAmount)
    }

    @Test
    fun settlingToZeroIsSettledInTheViewAndTheLedger() = runBlocking {
        due(TxnType.DUE_TO, 10_000, at = 100)
        settle(payeeId, theyPaidYou = true, amount = 10_000, transactedAt = 200)

        val row = viewRow().getById(payeeId)!!
        val ledger = ObservePayeeLedger(transactions)(payeeId).first()

        assertEquals(0L, row.dueToAmount)
        assertEquals(0L, row.dueFromAmount)
        assertEquals(BalanceDirection.SETTLED, ledger.direction)
        assertEquals(null, ledger.oldestOpenAt)
    }

    @Test
    fun ledgerAndViewAgreeOnAMixOfItems() = runBlocking {
        due(TxnType.DUE_TO, 10_000, at = 100)
        due(TxnType.DUE_FROM, 3_000, at = 200)
        settle(payeeId, theyPaidYou = true, amount = 5_000, transactedAt = 300)
        due(TxnType.DUE_TO, 2_000, at = 400)

        val ledger = ObservePayeeLedger(transactions)(payeeId).first()
        val row = viewRow().getById(payeeId)!!

        assertEquals(ledger.net, row.netBalanceAmount())
        assertEquals(4_000L, ledger.net)
        assertEquals(listOf(10_000L, 7_000L, 2_000L, 4_000L), ledger.history.map { it.runningBalance })
    }

    @Test
    fun sharedBalancesSummarisesEveryPayee() = runBlocking {
        val bilal = db.payeeDao().upsert(PayeeEntity(name = "Bilal", avatarUri = null, phone = null, upiId = null, createdAt = 0))
        due(TxnType.DUE_TO, 6_000, at = 100)
        transactions.save(
            TransactionEntity(
                amount = 3_000, type = TxnType.DUE_FROM, transactedAt = 50, createdAt = 50, updatedAt = 50,
                categoryId = food, payeeId = bilal, accountId = null, note = null, receiptUri = null
            )
        )

        val summary = ObserveSharedBalances(
            com.saadm.zenith.data.repository.PayeeRepoImpl(
                db.payeeDao(), db.payeeBalanceDao(), db.transactionDao()
            ),
            transactions
        )().first()

        assertEquals(6_000L, summary.owedToYou)
        assertEquals(3_000L, summary.youOwe)
        assertEquals(listOf("Bilal", "Asha"), summary.oldestFirst.map { it.name })
    }
}
