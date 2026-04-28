package com.saadm.zenith.data.db

import androidx.room.Database
import androidx.room.migration.Migration
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.saadm.zenith.data.db.converter.DbTypeConverters
import com.saadm.zenith.data.db.dao.AccountDao
import com.saadm.zenith.data.db.dao.BudgetDao
import com.saadm.zenith.data.db.dao.CategoryDao
import com.saadm.zenith.data.db.dao.PayeeDao
import com.saadm.zenith.data.db.dao.TransactionDao
import com.saadm.zenith.data.entity.AccountEntity
import com.saadm.zenith.data.entity.BudgetEntity
import com.saadm.zenith.data.entity.CategoryEntity
import com.saadm.zenith.data.entity.PayeeEntity
import com.saadm.zenith.data.entity.TransactionEntity
import com.saadm.zenith.data.entity.TxnType

@Database(
	entities = [
		TransactionEntity::class,
		PayeeEntity::class,
		CategoryEntity::class,
		AccountEntity::class,
		BudgetEntity::class
	],
	version = 4,
	exportSchema = false
)
@TypeConverters(DbTypeConverters::class)
abstract class AppDatabase : RoomDatabase() {
	companion object {
		private data class SeedCategory(
			val name: String,
			val emoji: String,
			val colorHex: String,
			val txnType: String,
			val sortOrder: Int,
			val isDefault: Boolean
		)

		private val DEFAULT_CATEGORIES = listOf(
			SeedCategory("Uncategorized", "TAG", "#9E9E9E", "EXPENSE", 0, true),
			SeedCategory("Uncategorized", "TAG", "#9E9E9E", "INCOME", 1, true),
			SeedCategory("Food", "DINE", "#FF7043", "EXPENSE", 2, false),
			SeedCategory("Transport", "COMMUTE", "#42A5F5", "EXPENSE", 3, false),
			SeedCategory("Utilities", "BILL", "#7E57C2", "EXPENSE", 4, false),
			SeedCategory("Misc", "MORE", "#66BB6A", "EXPENSE", 5, false),
			SeedCategory("Salary", "💰", "#4CAF50", "INCOME", 6, false),
			SeedCategory("Bonus", "🎁", "#FF9800", "INCOME", 7, false)
		)

		private fun seedDefaultCategories(db: SupportSQLiteDatabase) {
			DEFAULT_CATEGORIES.forEach { category ->
				db.execSQL(
					"""
					INSERT INTO categories (name, emoji, colorHex, txnType, sortOrder, isDefault, isDeleted)
					SELECT '${category.name}', '${category.emoji}', '${category.colorHex}', '${category.txnType}', ${category.sortOrder}, ${if (category.isDefault) 1 else 0}, 0
					WHERE NOT EXISTS (
						SELECT 1 FROM categories WHERE LOWER(name) = LOWER('${category.name}') AND txnType = '${category.txnType}'
					)
					""".trimIndent()
				)
			}
		}

		val MIGRATION_1_2 = object : Migration(1, 2) {
			override fun migrate(db: SupportSQLiteDatabase) {
				seedDefaultCategories(db)

				db.execSQL(
					"""
					CREATE TABLE IF NOT EXISTS transactions_new (
						id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
						amount INTEGER NOT NULL,
						type TEXT NOT NULL,
						transactedAt INTEGER NOT NULL,
						createdAt INTEGER NOT NULL,
						updatedAt INTEGER NOT NULL,
						categoryId INTEGER NOT NULL,
						payeeId INTEGER,
						accountId INTEGER,
						note TEXT,
						receiptUri TEXT,
						currency TEXT NOT NULL,
						isDeleted INTEGER NOT NULL,
						deletedAt INTEGER,
						FOREIGN KEY(payeeId) REFERENCES payees(id) ON DELETE SET NULL,
						FOREIGN KEY(accountId) REFERENCES accounts(id) ON DELETE SET NULL,
						FOREIGN KEY(categoryId) REFERENCES categories(id) ON DELETE RESTRICT
					)
					""".trimIndent()
				)

				db.execSQL(
					"""
					INSERT INTO transactions_new (
						id, amount, type, transactedAt, createdAt, updatedAt,
						categoryId, payeeId, accountId, note, receiptUri, currency, isDeleted, deletedAt
					)
					SELECT
						id, amount, type, transactedAt, createdAt, updatedAt,
						COALESCE(categoryId, (SELECT id FROM categories ORDER BY isDefault DESC, sortOrder ASC, id ASC LIMIT 1)),
						payeeId, accountId, note, receiptUri, currency, isDeleted, deletedAt
					FROM transactions
					""".trimIndent()
				)

				db.execSQL("DROP TABLE transactions")
				db.execSQL("ALTER TABLE transactions_new RENAME TO transactions")
				db.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_payeeId ON transactions(payeeId)")
				db.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_accountId ON transactions(accountId)")
				db.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_categoryId ON transactions(categoryId)")
				db.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_transactedAt ON transactions(transactedAt)")
				db.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_isDeleted ON transactions(isDeleted)")
			}
		}

		val MIGRATION_2_3 = object : Migration(2, 3) {
			override fun migrate(db: SupportSQLiteDatabase) {
				seedDefaultCategories(db)
			}
		}

		val MIGRATION_3_4 = object : Migration(3, 4) {
			override fun migrate(db: SupportSQLiteDatabase) {
				// Create new categories table with txnType column
				db.execSQL(
					"""
					CREATE TABLE IF NOT EXISTS categories_new (
						id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
						name TEXT NOT NULL,
						emoji TEXT NOT NULL,
						colorHex TEXT NOT NULL,
						txnType TEXT NOT NULL,
						sortOrder INTEGER NOT NULL,
						isDefault INTEGER NOT NULL,
						isDeleted INTEGER NOT NULL
					)
					""".trimIndent()
				)

				// Migrate existing categories, splitting BOTH into EXPENSE and INCOME
				db.execSQL(
					"""
					INSERT INTO categories_new (id, name, emoji, colorHex, txnType, sortOrder, isDefault, isDeleted)
					SELECT 
						id, 
						name, 
						emoji, 
						colorHex,
						CASE 
							WHEN applicableTo = 'BOTH' THEN 'EXPENSE'
							WHEN applicableTo = 'EXPENSE' THEN 'EXPENSE'
							WHEN applicableTo = 'INCOME' THEN 'INCOME'
							ELSE 'EXPENSE'
						END,
						sortOrder,
						CASE 
							WHEN applicableTo = 'BOTH' AND isDefault = 1 THEN 1
							ELSE isDefault
						END,
						isDeleted
					FROM categories
					WHERE applicableTo != 'BOTH' OR isDefault = 0
					""".trimIndent()
				)

				// For BOTH categories that are default, also create an INCOME variant
				db.execSQL(
					"""
					INSERT INTO categories_new (name, emoji, colorHex, txnType, sortOrder, isDefault, isDeleted)
					SELECT 
						name, 
						emoji, 
						colorHex,
						'INCOME',
						sortOrder + 1000,
						isDefault,
						isDeleted
					FROM categories
					WHERE applicableTo = 'BOTH'
					""".trimIndent()
				)

				// Drop old table and rename
				db.execSQL("DROP TABLE categories")
				db.execSQL("ALTER TABLE categories_new RENAME TO categories")

				// Seed the new typed defaults
				seedDefaultCategories(db)
			}
		}

		val DB_CREATE_CALLBACK = object : Callback() {
			override fun onCreate(db: SupportSQLiteDatabase) {
				super.onCreate(db)
				seedDefaultCategories(db)
			}
		}
	}

	// ...existing code...


	abstract fun transactionDao(): TransactionDao
	abstract fun payeeDao(): PayeeDao
	abstract fun categoryDao(): CategoryDao
	abstract fun accountDao(): AccountDao
	abstract fun budgetDao(): BudgetDao
}
