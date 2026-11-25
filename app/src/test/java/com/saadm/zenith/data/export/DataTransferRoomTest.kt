package com.saadm.zenith.data.export

import androidx.room.Room
import com.saadm.zenith.data.db.AppDatabase
import com.saadm.zenith.data.entity.AccountEntity
import com.saadm.zenith.data.entity.AccountType
import com.saadm.zenith.data.entity.CategoryEntity
import com.saadm.zenith.data.entity.PayeeEntity
import com.saadm.zenith.data.entity.TransactionEntity
import com.saadm.zenith.data.entity.TxnType
import com.saadm.zenith.domain.model.TxnFilter
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DataTransferRoomTest {

    private lateinit var db: AppDatabase
    private lateinit var transfer: DataTransferImpl
    private val food = 3L

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(RuntimeEnvironment.getApplication(), AppDatabase::class.java)
            .allowMainThreadQueries()
            .addCallback(AppDatabase.DB_CREATE_CALLBACK)
            .build()
        transfer = DataTransferImpl(db)
    }

    @After
    fun tearDown() {
        db.close()
    }

    private suspend fun seed() {
        val cash = db.accountDao().insert(
            AccountEntity(name = "Cash", type = AccountType.CASH, colorHex = "#111111", emoji = "C", sortOrder = 0, openingBalance = 500_000)
        )
        db.accountDao().insert(
            AccountEntity(name = "Old card", type = AccountType.CREDIT_CARD, colorHex = "#222222", emoji = "O", sortOrder = 1, isDeleted = true)
        )
        db.categoryDao().upsert(
            CategoryEntity(name = "Groceries, big", emoji = "G", colorHex = "#333333", txnType = TxnType.EXPENSE, sortOrder = 9)
        )
        val asha = db.payeeDao().upsert(
            PayeeEntity(name = "Asha, R. \"AR\"", avatarUri = null, phone = "+91 99999", upiId = "asha@upi", createdAt = 10)
        )
        suspend fun txn(type: TxnType, amount: Long, at: Long, note: String?, payee: Long? = null, account: Long? = cash) {
            db.transactionDao().upsert(
                TransactionEntity(
                    amount = amount, type = type, transactedAt = at, createdAt = at, updatedAt = at,
                    categoryId = food, payeeId = payee, accountId = account, note = note, receiptUri = null
                )
            )
        }
        txn(TxnType.EXPENSE, 120_050, 1_700_000_000_000, "Lunch, \"Sagar\"\nsecond line")
        txn(TxnType.INCOME, 99_900, 1_700_000_100_000, "=SUM(A1)")
        txn(TxnType.DUE_TO, 500_000, 1_700_000_200_000, null, payee = asha, account = null)
        txn(TxnType.SETTLEMENT_FROM, 20_000, 1_700_000_300_000, "part payment", payee = asha)
    }

    private suspend fun exportJson(now: Long = 1_234L): String {
        val out = ByteArrayOutputStream()
        transfer.writeJson(out, now)
        return out.toString(Charsets.UTF_8.name())
    }

    @Test
    fun jsonRoundTripIntoAnEmptyDatabaseIsIdentical() = runBlocking<Unit> {
        seed()
        val before = exportJson()

        db.clearAllTables()
        transfer.readJson(ByteArrayInputStream(before.toByteArray(Charsets.UTF_8)))

        assertEquals(before, exportJson())
    }

    @Test
    fun csvEscapesFieldsAndGuardsFormulas() = runBlocking<Unit> {
        seed()
        val out = ByteArrayOutputStream()
        transfer.writeCsv(out, TxnFilter.ALL)
        val csv = out.toString(Charsets.UTF_8.name())

        assertTrue(csv.startsWith("date,type,amount,currency,category,payee,account,note\n"))
        // A comma, a quote and a newline in a note: the field is quoted and quotes are doubled.
        assertTrue(csv.contains("\"Lunch, \"\"Sagar\"\"\nsecond line\""))
        // A note starting with = is prefixed so a spreadsheet shows text, not a formula.
        assertTrue(csv.contains(",'=SUM(A1)"))
        // Amounts are rupees with two decimals.
        assertTrue(csv.contains(",EXPENSE,1200.50,"))
    }

    @Test
    fun csvFilterKeepsOnlyTheChosenKind() = runBlocking<Unit> {
        seed()
        val out = ByteArrayOutputStream()
        transfer.writeCsv(out, TxnFilter.SETTLEMENTS)
        val csv = out.toString(Charsets.UTF_8.name())

        assertTrue(csv.contains("SETTLEMENT_FROM"))
        assertFalse(csv.contains(",EXPENSE,"))
        assertFalse(csv.contains("DUE_TO"))
    }

    @Test
    fun readingSomethingElseIsRefused() = runBlocking<Unit> {
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { transfer.readJson(ByteArrayInputStream("""{"format":"other","version":1}""".toByteArray())) }
        }
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { transfer.readJson(ByteArrayInputStream("""{"format":"zenith","version":99}""".toByteArray())) }
        }
    }
}
