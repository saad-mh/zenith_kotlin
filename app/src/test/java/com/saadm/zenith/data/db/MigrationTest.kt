package com.saadm.zenith.data.db

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.platform.app.InstrumentationRegistry
import com.saadm.zenith.data.db.migrations.Migrations
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Upgrades a real database from each exported older schema to the current one. Version 6 is what
 * builds before the v2 work used; its schema was generated from that code.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), AppDatabase::class.java)

    private fun SupportSQLiteDatabase.seed() {
        execSQL("INSERT INTO categories (id, name, emoji, colorHex, txnType, sortOrder, isDefault, isDeleted) VALUES (1, 'Food', 'DINE', '#000000', 'EXPENSE', 0, 1, 0)")
        execSQL("INSERT INTO payees (id, name, avatarUri, phone, upiId, createdAt, isDeleted) VALUES (1, 'Dev', NULL, NULL, NULL, 1, 0)")
        execSQL("INSERT INTO accounts (id, name, type, colorHex, emoji, sortOrder, isDeleted) VALUES (1, 'Cash', 'CASH', '#000000', 'C', 0, 0)")
        listOf(1 to ("DUE_TO" to 500), 2 to ("SETTLEMENT_FROM" to 200)).forEach { (id, row) ->
            execSQL(
                "INSERT INTO transactions (id, amount, type, transactedAt, createdAt, updatedAt, categoryId, payeeId, accountId, note, receiptUri, currency, isDeleted, deletedAt) " +
                    "VALUES ($id, ${row.second}, '${row.first}', 1, 1, 1, 1, 1, 1, NULL, NULL, 'INR', 0, NULL)"
            )
        }
    }

    private fun SupportSQLiteDatabase.long(sql: String): Long = query(sql).use {
        it.moveToFirst()
        it.getLong(0)
    }

    private fun upgradeFrom(version: Int): SupportSQLiteDatabase {
        helper.createDatabase(name, version).apply {
            seed()
            close()
        }
        // Also checks that the result matches the current schema exactly.
        return helper.runMigrationsAndValidate(name, 8, true, Migrations.MIGRATION_6_7, Migrations.MIGRATION_7_8)
    }

    private fun check(db: SupportSQLiteDatabase) {
        assertEquals(2L, db.long("SELECT COUNT(*) FROM transactions"))
        assertEquals(0L, db.long("SELECT openingBalance FROM accounts WHERE id = 1"))
        // 500 owed, 200 settled: the rebuilt view nets the two.
        assertEquals(300L, db.long("SELECT dueToAmount - dueFromAmount FROM payee_balances WHERE id = 1"))
    }

    @Test
    fun fromVersion6KeepsDataAndNetsSettlements() = check(upgradeFrom(6))

    @Test
    fun fromVersion7KeepsData() = check(upgradeFrom(7))

    // The helper compares the name it is given with the file path it opens, so pass the path.
    private val name get() = InstrumentationRegistry.getInstrumentation().targetContext.getDatabasePath("migration-test").path
}
