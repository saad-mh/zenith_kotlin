package com.saadm.zenith.data.db

import androidx.room.Room
import com.saadm.zenith.data.entity.BudgetEntity
import com.saadm.zenith.data.entity.BudgetPeriod
import com.saadm.zenith.data.entity.CategoryEntity
import com.saadm.zenith.data.entity.TransactionEntity
import com.saadm.zenith.data.entity.TxnType
import com.saadm.zenith.data.repository.CategoryRepoImpl
import com.saadm.zenith.ui.categories.nameProblem
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/** Merging and archiving categories, against a real in-memory database. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CategoryMergeRoomTest {

    private lateinit var db: AppDatabase
    private lateinit var repo: CategoryRepoImpl

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(RuntimeEnvironment.getApplication(), AppDatabase::class.java)
            .allowMainThreadQueries()
            .addCallback(AppDatabase.DB_CREATE_CALLBACK)
            .build()
        repo = CategoryRepoImpl(db.categoryDao())
    }

    @After
    fun tearDown() {
        db.close()
    }

    private suspend fun category(name: String) =
        repo.save(CategoryEntity(name = name, emoji = "", colorHex = "#000000", txnType = TxnType.EXPENSE, sortOrder = 9))

    private suspend fun budget(categoryId: Long, start: Long, limit: Long) = db.budgetDao().upsert(
        BudgetEntity(
            categoryId = categoryId, limitAmount = limit, periodType = BudgetPeriod.MONTHLY,
            startDate = start, endDate = start + 1
        )
    )

    @Test
    fun mergeMovesEntriesAndKeepsTheTargetsBudget() = runBlocking<Unit> {
        val from = category("Coffee")
        val into = category("Cafes")
        db.transactionDao().upsert(
            TransactionEntity(
                amount = 100, type = TxnType.EXPENSE, transactedAt = 1, createdAt = 1, updatedAt = 1,
                categoryId = from, payeeId = null, accountId = null, note = null, receiptUri = null
            )
        )
        budget(from, start = 1, limit = 10)
        budget(into, start = 1, limit = 20)
        budget(from, start = 2, limit = 30)

        repo.merge(from, into)

        assertEquals(into, db.transactionDao().observeAllActive().first().single().categoryId)
        assertEquals(listOf(20L), db.budgetDao().getForPeriod(1).map { it.limitAmount })
        assertEquals(listOf(into), db.budgetDao().getForPeriod(2).map { it.categoryId })
        assertEquals(true, repo.observeAll().first().single { it.id == from }.isDeleted)
    }

    @Test
    fun savingAnExistingCategoryKeepsItsId() = runBlocking<Unit> {
        val id = category("Coffee")
        val saved = repo.observeAll().first().single { it.id == id }
        repo.save(saved.copy(name = "Cafes"))
        assertEquals("Cafes", repo.observeAll().first().single { it.id == id }.name)
    }

    @Test
    fun namesMustBeNewAndNotBlank() {
        val existing = listOf(CategoryEntity(1, "Food", "", "#000000", TxnType.EXPENSE, 0))
        assertEquals("That name is already used", nameProblem(" food ", existing, null))
        assertEquals("Enter a name", nameProblem("  ", existing, null))
        assertNull(nameProblem("Food", existing, 1))
    }
}
