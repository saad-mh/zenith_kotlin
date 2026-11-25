package com.saadm.zenith.data.db

import androidx.room.Room
import com.saadm.zenith.data.entity.PayeeEntity
import com.saadm.zenith.data.entity.TransactionEntity
import com.saadm.zenith.data.entity.TxnType
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

/**
 * Editing a payee or a category must keep its transactions linked. REPLACE used to delete the row
 * first, which nulled the payee on every transaction, or failed outright on categories.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PayeeEditRoomTest {

    private lateinit var db: AppDatabase

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(RuntimeEnvironment.getApplication(), AppDatabase::class.java)
            .allowMainThreadQueries()
            .addCallback(AppDatabase.DB_CREATE_CALLBACK)
            .build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun editingAPayeeKeepsItsTransactions() = runBlocking<Unit> {
        val id = db.payeeDao().upsert(PayeeEntity(name = "Asha", avatarUri = null, phone = null, upiId = null, createdAt = 0))
        db.transactionDao().upsert(
            TransactionEntity(
                amount = 5_000, type = TxnType.DUE_TO, transactedAt = 1, createdAt = 1, updatedAt = 1,
                categoryId = 3L, payeeId = id, accountId = null, note = null, receiptUri = null
            )
        )

        // The same call the people screen makes when a payee is edited.
        db.payeeDao().upsert(PayeeEntity(id = id, name = "Asha R", avatarUri = null, phone = null, upiId = null, createdAt = 0))

        assertEquals(1, db.transactionDao().getActiveByPayeeId(id).size)
    }

    @Test
    fun renamingACategoryWithTransactionsSucceeds() = runBlocking<Unit> {
        // Seeded categories: 3 is Food. Its transactions must survive a rename.
        db.transactionDao().upsert(
            TransactionEntity(
                amount = 1_200, type = TxnType.EXPENSE, transactedAt = 1, createdAt = 1, updatedAt = 1,
                categoryId = 3L, payeeId = null, accountId = null, note = null, receiptUri = null
            )
        )
        val food = db.categoryDao().observeAllActive().first { list -> list.any { it.id == 3L } }.first { it.id == 3L }

        // The same call the settings category screen makes when a category is edited.
        db.categoryDao().upsert(food.copy(name = "Groceries"))

        val renamed = db.categoryDao().observeAllActive().first { list -> list.any { it.id == 3L } }.first { it.id == 3L }
        assertEquals("Groceries", renamed.name)
        assertEquals(1L, db.transactionDao().observeBetween(0, Long.MAX_VALUE).first().size.toLong())
    }
}
