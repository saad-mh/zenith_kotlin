package com.saadm.zenith.data.backup

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.room.Room
import com.saadm.zenith.data.db.AppDatabase
import com.saadm.zenith.data.entity.AccountEntity
import com.saadm.zenith.data.entity.AccountType
import com.saadm.zenith.data.entity.BudgetEntity
import com.saadm.zenith.data.entity.BudgetPeriod
import com.saadm.zenith.data.entity.CategoryEntity
import com.saadm.zenith.data.entity.PayeeEntity
import com.saadm.zenith.data.entity.TransactionEntity
import com.saadm.zenith.data.entity.TxnType
import com.saadm.zenith.data.export.DataTransferImpl
import com.saadm.zenith.data.preferences.AppPreferencesStore
import com.saadm.zenith.domain.export.DataTransfer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
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
import java.io.File
import java.io.OutputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class LocalBackupRoomTest {

    private lateinit var db: AppDatabase
    private lateinit var transfer: DataTransferImpl
    private lateinit var backup: LocalBackupImpl
    private lateinit var cacheDir: File
    private val passphrase = "correct horse battery"

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(RuntimeEnvironment.getApplication(), AppDatabase::class.java)
            .allowMainThreadQueries()
            .addCallback(AppDatabase.DB_CREATE_CALLBACK)
            .build()
        cacheDir = RuntimeEnvironment.getApplication().cacheDir
        transfer = DataTransferImpl(db)
        backup = LocalBackupImpl(db, transfer, AppPreferencesStore(InMemoryPreferencesStore()), cacheDir)
    }

    @After
    fun tearDown() {
        db.close()
    }

    private suspend fun seed() {
        val cash = db.accountDao().insert(
            AccountEntity(name = "Cash", type = AccountType.CASH, colorHex = "#111111", emoji = "C", sortOrder = 0, openingBalance = 500_000)
        )
        db.categoryDao().upsert(CategoryEntity(name = "Groceries", emoji = "G", colorHex = "#333333", txnType = TxnType.EXPENSE, sortOrder = 9))
        val asha = db.payeeDao().upsert(PayeeEntity(name = "Asha", avatarUri = null, phone = null, upiId = "asha@upi", createdAt = 10))
        db.transactionDao().upsert(
            TransactionEntity(
                amount = 120_050, type = TxnType.EXPENSE, transactedAt = 1_700_000_000_000, createdAt = 1, updatedAt = 1,
                categoryId = 3L, payeeId = null, accountId = cash, note = "Lunch, \"Sagar\"", receiptUri = null
            )
        )
        db.transactionDao().upsert(
            TransactionEntity(
                amount = 500_000, type = TxnType.DUE_TO, transactedAt = 1_700_000_100_000, createdAt = 1, updatedAt = 1,
                categoryId = 3L, payeeId = asha, accountId = null, note = null, receiptUri = null
            )
        )
        db.budgetDao().upsert(
            BudgetEntity(categoryId = null, limitAmount = 2_000_000, periodType = BudgetPeriod.MONTHLY, startDate = 1_700_000_000_000, endDate = 1_702_000_000_000)
        )
    }

    private suspend fun exportJson(): String {
        val out = ByteArrayOutputStream()
        transfer.writeJson(out, now = 1L)
        return out.toString(Charsets.UTF_8.name())
    }

    private suspend fun writeBackup(source: DataTransfer = transfer): ByteArray {
        val out = ByteArrayOutputStream()
        LocalBackupImpl(db, source, AppPreferencesStore(InMemoryPreferencesStore()), cacheDir)
            .writeBackup(out, passphrase.toCharArray())
        return out.toByteArray()
    }

    @Test
    fun backupWipeRestoreReturnsAnIdenticalDatabase() = runBlocking<Unit> {
        seed()
        val before = exportJson()
        val encrypted = writeBackup()

        backup.deleteAllData("DELETE")
        assertEquals(0, db.transactionDao().observeAllActive().first().size)

        backup.restoreBackup(ByteArrayInputStream(encrypted), passphrase.toCharArray(), replaceEverything = true)

        assertEquals(before, exportJson())
    }

    @Test
    fun aWrongPassphraseChangesNothing() = runBlocking<Unit> {
        seed()
        val before = exportJson()
        val encrypted = writeBackup()

        assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                backup.restoreBackup(ByteArrayInputStream(encrypted), "wrong passphrase".toCharArray(), replaceEverything = true)
            }
        }
        assertEquals(before, exportJson())
    }

    @Test
    fun aDamagedBackupChangesNothing() = runBlocking<Unit> {
        seed()
        val before = exportJson()
        val encrypted = writeBackup()
        encrypted[encrypted.size - 5] = (encrypted[encrypted.size - 5].toInt() xor 0x01).toByte()

        assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                backup.restoreBackup(ByteArrayInputStream(encrypted), passphrase.toCharArray(), replaceEverything = true)
            }
        }
        assertEquals(before, exportJson())
    }

    @Test
    fun aValidBackupOfBrokenDataRollsTheRestoreBack() = runBlocking<Unit> {
        seed()
        val before = exportJson()
        // Encrypted correctly, but the contents are cut short, so the import fails midway.
        val truncating = object : DataTransfer by transfer {
            override suspend fun writeJson(output: OutputStream, now: Long) {
                output.write("""{"format":"zenith","version":1,"categories":[""".toByteArray())
            }
        }
        val encrypted = writeBackup(truncating)

        assertThrows(Exception::class.java) {
            runBlocking {
                backup.restoreBackup(ByteArrayInputStream(encrypted), passphrase.toCharArray(), replaceEverything = true)
            }
        }
        assertEquals(before, exportJson())
    }

    @Test
    fun aFileThatIsNotABackupIsRefused() = runBlocking<Unit> {
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                backup.restoreBackup(
                    ByteArrayInputStream("this is a plain text file, not a backup at all".toByteArray()),
                    passphrase.toCharArray(),
                    replaceEverything = true
                )
            }
        }
    }

    @Test
    fun restoreRequiresTheReplaceConfirmation() = runBlocking<Unit> {
        val encrypted = writeBackup()
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                backup.restoreBackup(ByteArrayInputStream(encrypted), passphrase.toCharArray(), replaceEverything = false)
            }
        }
    }

    @Test
    fun shortPassphrasesAreRefused() = runBlocking<Unit> {
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { backup.writeBackup(ByteArrayOutputStream(), "short".toCharArray()) }
        }
    }

    @Test
    fun deleteAllNeedsTheTypedWord() = runBlocking<Unit> {
        seed()
        val before = exportJson()

        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { backup.deleteAllData("delete") }
        }
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { backup.deleteAllData("") }
        }
        assertEquals(before, exportJson())

        backup.deleteAllData("DELETE")
        // Only the seeded categories remain, with a default for each kind.
        assertNotNull(db.categoryDao().getDefaultByType("EXPENSE"))
        assertNotNull(db.categoryDao().getDefaultByType("INCOME"))
        assertTrue(db.categoryDao().getAll().none { it.name == "Food & dining" })
        assertEquals(0, db.transactionDao().observeAllActive().first().size)
    }

    @Test
    fun clearCacheReportsTheBytesFreed() = runBlocking<Unit> {
        val folder = File(cacheDir, "test-cache").apply { mkdirs() }
        File(folder, "a.bin").writeBytes(ByteArray(100))
        File(folder, "b.bin").writeBytes(ByteArray(28))

        val freed = backup.clearCache()

        assertTrue(freed >= 128)
        assertEquals(0, cacheDir.listFiles()?.size ?: 0)
    }

    private class InMemoryPreferencesStore(initial: Preferences = emptyPreferences()) : DataStore<Preferences> {
        private val state = MutableStateFlow(initial)
        override val data: Flow<Preferences> = state

        override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences {
            state.value = transform(state.value)
            return state.value
        }
    }
}
