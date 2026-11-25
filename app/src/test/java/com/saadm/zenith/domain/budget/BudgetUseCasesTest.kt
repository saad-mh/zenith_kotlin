package com.saadm.zenith.domain.budget

import com.saadm.zenith.data.entity.BudgetEntity

import com.saadm.zenith.data.entity.TransactionEntity
import com.saadm.zenith.data.entity.TxnType
import com.saadm.zenith.domain.model.MonthPeriod
import com.saadm.zenith.domain.model.MonthPeriods
import com.saadm.zenith.domain.repository.BudgetRepo
import com.saadm.zenith.domain.repository.TransactionRepo
import com.saadm.zenith.domain.usecase.CopyBudgetsFromPreviousMonth
import com.saadm.zenith.domain.usecase.HideBudget
import com.saadm.zenith.domain.usecase.ObserveMonthBudgets
import com.saadm.zenith.domain.usecase.SaveBudget
import com.saadm.zenith.domain.usecase.SuggestBudgets
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class BudgetUseCasesTest {

    private val zone: ZoneId = ZoneId.of("Asia/Kolkata")
    private val january = MonthPeriods.containing(LocalDate.of(2026, 1, 10), zone = zone)
    private val february = MonthPeriods.containing(LocalDate.of(2026, 2, 10), zone = zone)
    private val today = LocalDate.of(2026, 2, 14)

    private lateinit var budgets: FakeBudgetRepo
    private lateinit var transactions: FakeTransactionRepo

    @org.junit.Before
    fun setUp() {
        budgets = FakeBudgetRepo()
        transactions = FakeTransactionRepo()
    }

    @Test
    fun saveRejectsPastMonthsAndNonPositiveLimits() = runBlocking {
        val save = SaveBudget(budgets)
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { save(january, today, categoryId = 3L, limit = 100_00) }
        }
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { save(february, today, categoryId = 3L, limit = 0) }
        }
        assertEquals(0, budgets.rows.size)
    }

    @Test
    fun savingTheSameCategoryTwiceUpdatesOneRow() = runBlocking {
        val save = SaveBudget(budgets)
        save(february, today, categoryId = 3L, limit = 500_00)
        save(february, today, categoryId = 3L, limit = 800_00)

        assertEquals(1, budgets.rows.size)
        assertEquals(800_00L, budgets.rows.single().limitAmount)
    }

    @Test
    fun copyFillsAnEmptyMonthFromLastMonthOnly() = runBlocking {
        SaveBudget(budgets).invoke(january, LocalDate.of(2026, 1, 10), categoryId = null, limit = 20_000_00)
        SaveBudget(budgets).invoke(january, LocalDate.of(2026, 1, 10), categoryId = 3L, limit = 4_000_00)

        val copy = CopyBudgetsFromPreviousMonth(budgets)
        assertEquals(2, copy(february, today, zone))
        assertEquals(2, budgets.getForPeriod(february).size)
        // Once a month has budgets, copying does nothing.
        assertEquals(0, copy(february, today, zone))
        assertEquals(2, budgets.getForPeriod(february).size)
    }

    @Test
    fun budgetsDoNotCarryIntoTheNextMonth() = runBlocking {
        SaveBudget(budgets)(january, LocalDate.of(2026, 1, 10), categoryId = null, limit = 20_000_00)
        transactions.add(TxnType.EXPENSE, 5_000_00, LocalDate.of(2026, 2, 3), categoryId = 3L)

        val march = MonthPeriods.next(february, zone)
        val summary = ObserveMonthBudgets(budgets, transactions)(march, LocalDate.of(2026, 3, 2)).first()

        assertNull(summary.overall)
        assertNull(summary.leftToSpend)
    }

    @Test
    fun hiddenBudgetsDropOutOfTheMonth() = runBlocking {
        val id = SaveBudget(budgets)(february, today, categoryId = 3L, limit = 500_00)
        HideBudget(budgets)(february, today, id)

        assertEquals(0, ObserveMonthBudgets(budgets, transactions)(february, today).first().categories.size)
    }

    @Test
    fun pastMonthsCannotBeCopiedInto() = runBlocking<Unit> {
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { CopyBudgetsFromPreviousMonth(budgets)(january, today, zone) }
        }
    }

    @Test
    fun suggestionsAverageThreeMonthsPerCategory() = runBlocking {
        // Nov, Dec and Jan of food spend: 30,000 + 60,000 + 90,000 rupees (in paise), averaging 60,000.
        transactions.add(TxnType.EXPENSE, 3_000_00, LocalDate.of(2025, 11, 5), categoryId = 3L)
        transactions.add(TxnType.EXPENSE, 6_000_00, LocalDate.of(2025, 12, 5), categoryId = 3L)
        transactions.add(TxnType.EXPENSE, 9_000_00, LocalDate.of(2026, 1, 5), categoryId = 3L)
        // Transport: 45 paise over three months is below ₹10 after the average, so it rounds to ₹20.
        transactions.add(TxnType.EXPENSE, 45_00, LocalDate.of(2026, 1, 6), categoryId = 4L)
        // Income and anything in February never counts toward a suggestion.
        transactions.add(TxnType.INCOME, 99_000_00, LocalDate.of(2026, 1, 7), categoryId = 3L)
        transactions.add(TxnType.EXPENSE, 50_000_00, LocalDate.of(2026, 2, 2), categoryId = 3L)

        val suggestions = SuggestBudgets(transactions)(february, zone)

        assertEquals(6_000_00L, suggestions[3L])
        assertEquals(2_000L, suggestions[4L])
        assertNull(suggestions[99L])
    }

    @Test
    fun monthSummaryFollowsTheEditableFlag() = runBlocking {
        SaveBudget(budgets)(february, today, categoryId = null, limit = 10_000_00)
        val open = ObserveMonthBudgets(budgets, transactions)(february, today).first()
        val closed = ObserveMonthBudgets(budgets, transactions)(february, LocalDate.of(2026, 3, 5)).first()

        assertEquals(true, open.editable)
        assertEquals(false, closed.editable)
    }

    private class FakeBudgetRepo : BudgetRepo {
        val rows = mutableListOf<BudgetEntity>()
        private var nextId = 1L

        override fun observeForPeriod(period: MonthPeriod): Flow<List<BudgetEntity>> = flowOf(activeFor(period))

        override suspend fun getForPeriod(period: MonthPeriod): List<BudgetEntity> = activeFor(period)

        override suspend fun save(budget: BudgetEntity): Long {
            val id = if (budget.id == 0L) nextId++ else budget.id
            rows.removeAll { it.id == id }
            rows += budget.copy(id = id)
            return id
        }

        override suspend fun deactivate(id: Long) {
            val index = rows.indexOfFirst { it.id == id }
            if (index >= 0) rows[index] = rows[index].copy(isActive = false)
        }

        private fun activeFor(period: MonthPeriod) =
            rows.filter { it.isActive && it.startDate == period.startMillis }
    }

    private class FakeTransactionRepo : TransactionRepo {
        private val items = mutableListOf<TransactionEntity>()
        private val zone = ZoneId.of("Asia/Kolkata")

        fun add(type: TxnType, amount: Long, date: LocalDate, categoryId: Long) {
            val at = date.atTime(12, 0).atZone(zone).toInstant().toEpochMilli()
            items += TransactionEntity(
                amount = amount, type = type, transactedAt = at, createdAt = at, updatedAt = at,
                categoryId = categoryId, payeeId = null, accountId = null, note = null, receiptUri = null
            )
        }

        override fun observeAllActive(): Flow<List<TransactionEntity>> = flowOf(items.toList())
        override suspend fun deleteAll(transactions: List<TransactionEntity>) {
            items.removeAll(transactions.toSet())
        }

        override fun getRecentTransactions(): Flow<List<TransactionEntity>> = flowOf(items.toList())
        override suspend fun delete(transaction: TransactionEntity) {}
        override suspend fun save(transaction: TransactionEntity): Long = 0L
        override suspend fun saveAll(transactions: List<TransactionEntity>): List<Long> = emptyList()
        override fun observeForPayee(payeeId: Long): Flow<List<TransactionEntity>> = flowOf(emptyList())
        override fun observeBetween(startMillis: Long, endMillis: Long): Flow<List<TransactionEntity>> =
            flowOf(items.filter { it.transactedAt >= startMillis && it.transactedAt < endMillis })
        override fun observeSearch(query: String): Flow<List<TransactionEntity>> = flowOf(emptyList())
    }
}
