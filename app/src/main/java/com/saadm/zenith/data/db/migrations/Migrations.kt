package com.saadm.zenith.data.db.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.saadm.zenith.data.entity.PayeeBalancesViewSql

/** Migrations added after version 6. Earlier ones stay in AppDatabase, where they were written. */
object Migrations {

    /**
     * Payee balances now net settlements against dues. The view changes shape, and nothing
     * in the table data does: settlements are new rows of existing (text) types.
     */
    val MIGRATION_6_7 = object : Migration(6, 7) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("DROP VIEW IF EXISTS payee_balances")
            db.execSQL("CREATE VIEW `payee_balances` AS ${PayeeBalancesViewSql.SELECT}")
        }
    }
}
