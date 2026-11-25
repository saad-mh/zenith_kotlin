package com.saadm.zenith.data.db

import androidx.room.Room
import com.saadm.zenith.data.entity.PayeeEntity
import com.saadm.zenith.data.entity.TransactionEntity
import com.saadm.zenith.data.entity.TxnType
import com.saadm.zenith.data.repository.TransactionRepoImpl
import com.saadm.zenith.domain.analytics.CategoryTotal
import com.saadm.zenith.domain.model.MonthPeriod
import com.saadm.zenith.domain.model.MonthPeriods
import com.saadm.zenith.domain.model.TxnFilter
import com.saadm.zenith.domain.usecase.ObserveMonthSummary
import com.saadm.zenith.domain.usecase.ObserveSpendByCategory
import com.saadm.zenith.domain.usecase.ObserveTransactions
import com.saadm.zenith.domain.usecase.SearchTransactions
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.ZoneId

/** Runs the real Room queries against an in-memory database, so the SQL and the boundaries are tested. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TransactionRepoRoomTest {

    private val zone: ZoneId = ZoneId.of("Asia/Kolkata")
    private lateinit var db: AppDatabase
    private lateinit var repo: TransactionRepoImpl

    // Seeded defaults, in insertion order: 1 Uncategorized (expense), 2 Uncategorized (income), 3 Food, 4 Transport.
    private val uncategorized = 1L
    private val food = 3L
    private val transport = 4L

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(RuntimeEnvironment.getApplication(), AppDatabase::class.java)
            .allowMainThreadQueries()
            .addCallback(AppDatabase.DB_CREATE_CALLBACK)
            .build()
        repo = TransactionRepoImpl(db.transactionDao())
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun millis(date: LocalDate, hour: Int = 12): Long =
        date.atTime(hour, 0).atZone(zone).toInstant().toEpochMilli()

    private suspend fun add(
        type: TxnType,
        amount: Long,
        date: LocalDate,
        categoryId: Long = food,
        note: String? = null,
        payeeId: Long? = null,
        isDeleted: Boolean = false,
        at: Long = millis(date)
    ): Long = db.transactionDao().upsert(
        TransactionEntity(
            amount = amount,
            type = type,
            transactedAt = at,
            createdAt = at,
            updatedAt = at,
            categoryId = categoryId,
            payeeId = payeeId,
            accountId = null,
            note = note,
            receiptUri = null,
            isDeleted = isDeleted
        )
    )

    private val february: MonthPeriod = MonthPeriods.containing(LocalDate.of(2026, 2, 15), startDay = 1, zone = zone)

    @Test
    fun periodQueryIncludesTheStartAndExcludesTheEnd() = runBlocking {
        add(TxnType.EXPENSE, 100, LocalDate.of(2026, 2, 1), at = february.startMillis)
        add(TxnType.EXPENSE, 200, LocalDate.of(2026, 2, 28), at = february.endMillis - 1)
        add(TxnType.EXPENSE, 400, LocalDate.of(2026, 3, 1), at = february.endMillis)
        add(TxnType.EXPENSE, 800, LocalDate.of(2026, 1, 31), at = february.startMillis - 1)

        val items = ObserveTransactions(repo)(february).first()

        assertEquals(listOf(200L, 100L), items.map { it.amount })
    }

    @Test
    fun deletedRowsAreHiddenFromEveryQuery() = runBlocking {
        add(TxnType.EXPENSE, 100, LocalDate.of(2026, 2, 3), note = "kept")
        add(TxnType.EXPENSE, 999, LocalDate.of(2026, 2, 3), note = "kept but deleted", isDeleted = true)

        assertEquals(1, ObserveTransactions(repo)(february).first().size)
        assertEquals(1, SearchTransactions(repo)("kept").first().size)
        assertEquals(100L, ObserveMonthSummary(repo)(february).first().expense)
    }

    @Test
    fun filtersSelectTheRightKinds() = runBlocking {
        add(TxnType.INCOME, 500, LocalDate.of(2026, 2, 2))
        add(TxnType.EXPENSE, 100, LocalDate.of(2026, 2, 3))
        add(TxnType.DUE_TO, 300, LocalDate.of(2026, 2, 4))
        add(TxnType.DUE_FROM, 250, LocalDate.of(2026, 2, 5))
        add(TxnType.SETTLEMENT_FROM, 60, LocalDate.of(2026, 2, 6))
        add(TxnType.SETTLEMENT_TO, 40, LocalDate.of(2026, 2, 7))

        val ui = ObserveTransactions(repo)
        assertEquals(6, ui(february).first().size)
        assertEquals(listOf(500L), ui(february, TxnFilter.INCOME).first().map { it.amount })
        assertEquals(listOf(100L), ui(february, TxnFilter.EXPENSE).first().map { it.amount })
        assertEquals(listOf(300L), ui(february, TxnFilter.OWES_YOU).first().map { it.amount })
        assertEquals(listOf(250L), ui(february, TxnFilter.YOU_OWE).first().map { it.amount })
        assertEquals(setOf(60L, 40L), ui(february, TxnFilter.SETTLEMENTS).first().map { it.amount }.toSet())
    }

    @Test
    fun searchMatchesNoteCategoryAndPayeeIgnoringCase() = runBlocking {
        val asha = db.payeeDao().upsert(PayeeEntity(name = "Asha Rao", avatarUri = null, phone = null, upiId = null, createdAt = 0))
        add(TxnType.EXPENSE, 100, LocalDate.of(2026, 2, 1), note = "Lunch at Sagar")
        add(TxnType.EXPENSE, 200, LocalDate.of(2026, 2, 2), categoryId = transport)
        add(TxnType.DUE_FROM, 300, LocalDate.of(2026, 2, 3), payeeId = asha)

        val search = SearchTransactions(repo)
        assertEquals(listOf(100L), search("sagar").first().map { it.amount })
        assertEquals(listOf(200L), search("TRANSPORT").first().map { it.amount })
        assertEquals(listOf(300L), search("asha").first().map { it.amount })
    }

    @Test
    fun searchEscapesWildcards() = runBlocking {
        add(TxnType.EXPENSE, 100, LocalDate.of(2026, 2, 1), note = "50% off")
        add(TxnType.EXPENSE, 200, LocalDate.of(2026, 2, 2), note = "500 off")
        add(TxnType.EXPENSE, 300, LocalDate.of(2026, 2, 3), note = "a_b")
        add(TxnType.EXPENSE, 400, LocalDate.of(2026, 2, 4), note = "axb")

        val search = SearchTransactions(repo)
        assertEquals(listOf(100L), search("50%").first().map { it.amount })
        assertEquals(listOf(300L), search("a_b").first().map { it.amount })
    }

    @Test
    fun blankSearchMatchesNothing() = runBlocking {
        add(TxnType.EXPENSE, 100, LocalDate.of(2026, 2, 1))
        assertTrue(SearchTransactions(repo)("   ").first().isEmpty())
    }

    @Test
    fun monthSummaryAndCategoryTotalsUseOnlyRealSpending() = runBlocking {
        add(TxnType.INCOME, 5_000_00, LocalDate.of(2026, 2, 1))
        add(TxnType.EXPENSE, 1_200_00, LocalDate.of(2026, 2, 3), categoryId = food)
        add(TxnType.EXPENSE, 300_00, LocalDate.of(2026, 2, 5), categoryId = transport)
        add(TxnType.DUE_FROM, 9_999_00, LocalDate.of(2026, 2, 6))

        val summary = ObserveMonthSummary(repo)(february).first()
        val byCategory = ObserveSpendByCategory(repo)(february).first()

        assertEquals(5_000_00L, summary.income)
        assertEquals(1_500_00L, summary.expense)
        assertEquals(listOf(CategoryTotal(food, 1_200_00), CategoryTotal(transport, 300_00)), byCategory)
    }

    @Test
    fun monthStartDaySetsWhichTransactionsCountAsThisMonth() = runBlocking {
        add(TxnType.EXPENSE, 100, LocalDate.of(2026, 2, 10))
        add(TxnType.EXPENSE, 200, LocalDate.of(2026, 2, 20))

        // With the month starting on the 15th, the 15th of February to the 14th of March is the period.
        val fifteenth = MonthPeriods.containing(LocalDate.of(2026, 2, 20), startDay = 15, zone = zone)
        val summary = ObserveMonthSummary(repo)(fifteenth).first()

        assertEquals(LocalDate.of(2026, 2, 15), fifteenth.startDate)
        assertEquals(200L, summary.expense)
    }
}
