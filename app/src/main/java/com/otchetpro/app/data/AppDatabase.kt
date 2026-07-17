package com.otchetpro.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [Report::class],
    version = 2,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun reportDao(): ReportDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // Миграция с версии 1 на 2 (добавляем индексы)
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Индекс для быстрого поиска по подразделению
                database.execSQL("CREATE INDEX IF NOT EXISTS idx_reports_dept ON reports(dept)")
                // Индекс для поиска по статусу
                database.execSQL("CREATE INDEX IF NOT EXISTS idx_reports_status ON reports(status)")
                // Индекс для сортировки по дате
                database.execSQL("CREATE INDEX IF NOT EXISTS idx_reports_created_at ON reports(createdAt)")
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "otchetpro.db"
                )
                .addMigrations(MIGRATION_1_2)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
