package com.saadm.zenith.data.db

import androidx.room.Room
import com.saadm.zenith.data.entity.BudgetEntity
import com.saadm.zenith.data.entity.BudgetPeriod
import com.saadm.zenith.data.repository.BudgetRepoImpl
import com.saadm.zenith.domain.model.MonthPeriod
import com.saadm.zenith.domain.model.MonthPeriods
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
import java.time.LocalDate
import java.time.ZoneId

/** Month scoping of budgets, checked against the real query. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BudgetDaoRoomTest {

    private val zone: ZoneId = ZoneId.of("Asia/Kolkata")
    private lateinit var db: AppDatabase
    private lateinit var repo: BudgetRepoImpl

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(RuntimeEnvironment.getApplication(), AppDatabase::class.java)
            .allowMainThreadQueries()
            .addCallback(AppDatabase.DB_CREATE_CALLBACK)
            .build()
        repo = BudgetRepoImpl(db.budgetDao())
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun budget(period: MonthPeriod, categoryId: Long?, limit: Long) = BudgetEntity(
        categoryId = categoryId,
        limitAmount = limit,
        periodType = BudgetPeriod.MONTHLY,
        startDate = period.startMillis,
        endDate = period.endMillis
    )

    @Test
    fun budgetsAreScopedToTheirMonth() = runBlocking {
        val february = MonthPeriods.containing(LocalDate.of(2026, 2, 10), zone = zone)
        val march = MonthPeriods.next(february, zone)
        repo.save(budget(february, categoryId = null, limit = 10_000_00))
        repo.save(budget(march, categoryId = 3L, limit = 2_000_00))

        assertEquals(listOf(10_000_00L), repo.getForPeriod(february).map { it.limitAmount })
        assertEquals(listOf(2_000_00L), repo.observeForPeriod(march).first().map { it.limitAmount })
    }

    @Test
    fun overallBudgetSortsBeforeCategoryBudgets() = runBlocking {
        val february = MonthPeriods.containing(LocalDate.of(2026, 2, 10), zone = zone)
        repo.save(budget(february, categoryId = 3L, limit = 500_00))
        repo.save(budget(february, categoryId = null, limit = 10_000_00))

        assertEquals(listOf(null, 3L), repo.getForPeriod(february).map { it.categoryId })
    }

    @Test
    fun hiddenBudgetsDropOutOfTheMonth() = runBlocking {
        val february = MonthPeriods.containing(LocalDate.of(2026, 2, 10), zone = zone)
        val id = repo.save(budget(february, categoryId = 3L, limit = 500_00))
        repo.deactivate(id)

        assertEquals(0, repo.getForPeriod(february).size)
        assertEquals(0, repo.observeForPeriod(february).first().size)
    }
}
