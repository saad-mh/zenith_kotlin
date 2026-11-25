package com.saadm.zenith.ui.settings

import androidx.room.withTransaction
import com.saadm.zenith.data.db.AppDatabase
import com.saadm.zenith.data.entity.AccountEntity
import com.saadm.zenith.data.entity.AccountType
import com.saadm.zenith.data.entity.BudgetEntity
import com.saadm.zenith.data.entity.BudgetPeriod
import com.saadm.zenith.data.entity.PayeeEntity
import com.saadm.zenith.data.entity.TransactionEntity
import com.saadm.zenith.data.entity.TxnType
import com.saadm.zenith.domain.model.MonthPeriods
import java.time.LocalDate
import java.time.ZoneId
import kotlin.random.Random

private val people = listOf("Aarav", "Meera", "Kabir", "Zoya", "Dev", "Nisha")
private val notes = listOf("Coffee", "Groceries", "Cab", "Dinner out", "Electricity", "Movie", "Snacks", "Fuel")

/**
 * Six months of random salary and spending, a few dues between friends and budgets for this month.
 * Returns how many entries were added. Nothing existing is changed or removed.
 */
suspend fun seedRandomData(database: AppDatabase, today: LocalDate, random: Random = Random.Default): Int =
    database.withTransaction {
        val categories = database.categoryDao().getAll().filter { !it.isDeleted }
        val spending = categories.filter { it.txnType == TxnType.EXPENSE }
        val income = categories.first { it.txnType == TxnType.INCOME }

        val accountId = database.accountDao().getAll().firstOrNull { !it.isDeleted }?.id
            ?: database.accountDao().insert(
                AccountEntity(
                    name = "Test bank", type = AccountType.BANK_TRANSFER, colorHex = "#5A5FB0", emoji = "",
                    sortOrder = 0, openingBalance = random.nextLong(20_000, 90_000) * 100
                )
            )
        val now = System.currentTimeMillis()
        val payees = database.payeeDao().getAll().filter { !it.isDeleted }.map { it.id }.ifEmpty {
            people.shuffled(random).take(3).map {
                database.payeeDao().upsert(PayeeEntity(name = it, avatarUri = null, phone = null, upiId = null, createdAt = now))
            }
        }

        fun entry(rupees: Long, type: TxnType, date: LocalDate, categoryId: Long, payeeId: Long? = null, note: String? = null): TransactionEntity {
            val at = date.atTime(random.nextInt(8, 22), random.nextInt(60)).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            return TransactionEntity(
                amount = rupees * 100, type = type, transactedAt = at.coerceAtMost(now), createdAt = now, updatedAt = now,
                categoryId = categoryId, payeeId = payeeId,
                accountId = accountId.takeIf { type == TxnType.EXPENSE || type == TxnType.INCOME },
                note = note, receiptUri = null
            )
        }

        val rows = mutableListOf<TransactionEntity>()
        repeat(6) { monthsBack ->
            val month = today.minusMonths(monthsBack.toLong())
            val lastDay = if (monthsBack == 0) today.dayOfMonth else month.lengthOfMonth()
            rows += entry(random.nextLong(60, 90) * 1000, TxnType.INCOME, month.withDayOfMonth(1), income.id, note = "Salary")
            repeat(random.nextInt(8, 14)) {
                rows += entry(
                    random.nextLong(80, 4000), TxnType.EXPENSE, month.withDayOfMonth(random.nextInt(1, lastDay + 1)),
                    spending.random(random).id, note = notes.random(random)
                )
            }
        }
        payees.take(3).forEach { payee ->
            val type = if (random.nextBoolean()) TxnType.DUE_TO else TxnType.DUE_FROM
            rows += entry(random.nextLong(100, 3000), type, today.minusDays(random.nextLong(1, 20)), spending.random(random).id, payee, "Split bill")
        }
        database.transactionDao().upsertAll(rows)

        val period = MonthPeriods.containing(today)
        val budgeted = database.budgetDao().getForPeriod(period.startMillis).map { it.categoryId }
        spending.filter { it.id !in budgeted }.shuffled(random).take(4).forEach { category ->
            val spent = rows.filter { it.categoryId == category.id && it.type == TxnType.EXPENSE && it.transactedAt >= period.startMillis }
                .sumOf { it.amount }
            // Aim around what was spent, so the budgets land on all three statuses.
            val limit = (spent * random.nextInt(80, 180) / 100).coerceAtLeast(100_000)
            database.budgetDao().upsert(
                BudgetEntity(
                    categoryId = category.id, limitAmount = limit, periodType = BudgetPeriod.MONTHLY,
                    startDate = period.startMillis, endDate = period.endMillis
                )
            )
        }
        rows.size
    }
