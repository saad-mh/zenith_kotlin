package com.saadm.zenith.ui.settings

import androidx.room.Room
import com.saadm.zenith.data.db.AppDatabase
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.time.LocalDate
import kotlin.random.Random

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SeedDataTest {
    // The first of a month is the tightest case: the current month has a single day to pick from.
    @Test
    fun seedingTwiceAddsEntriesWithoutDuplicatingAccountsOrBudgets() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(RuntimeEnvironment.getApplication(), AppDatabase::class.java)
            .allowMainThreadQueries()
            .addCallback(AppDatabase.DB_CREATE_CALLBACK)
            .build()
        val today = LocalDate.of(2026, 3, 1)
        val first = seedRandomData(db, today, Random(1))
        val second = seedRandomData(db, today, Random(2))
        assertTrue(first > 50)
        assertEquals(first + second, db.transactionDao().page(1_000, 0).size)
        assertEquals(1, db.accountDao().getAll().size)
        assertEquals(3, db.payeeDao().getAll().size)
        val budgeted = db.budgetDao().getAll().map { it.categoryId }
        assertEquals(budgeted.size, budgeted.toSet().size)
        db.close()
    }
}
