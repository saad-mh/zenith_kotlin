package com.saadm.zenith.data.db

import android.content.Context
import androidx.room.Room

object DatabaseProvider {
    @Volatile
    private var instance: AppDatabase? = null

    fun getInstance(context: Context): AppDatabase {
        return instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context,
                AppDatabase::class.java,
                "zenith.db"
            )
                .addMigrations(
                    AppDatabase.MIGRATION_1_2,
                    AppDatabase.MIGRATION_2_3,
                    AppDatabase.MIGRATION_3_4
                )
                .addCallback(AppDatabase.DB_CREATE_CALLBACK)
                .build()
                .also { instance = it }
        }
    }
}



