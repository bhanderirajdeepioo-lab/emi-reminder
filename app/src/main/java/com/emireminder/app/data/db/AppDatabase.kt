package com.emireminder.app.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.emireminder.app.data.db.dao.LoanDao
import com.emireminder.app.data.db.dao.ReminderDao
import com.emireminder.app.data.db.dao.SMSImportDao
import com.emireminder.app.data.db.entity.Loan
import com.emireminder.app.data.db.entity.Reminder
import com.emireminder.app.data.db.entity.SMSImport

@Database(
    entities = [Loan::class, Reminder::class, SMSImport::class],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun loanDao(): LoanDao
    abstract fun reminderDao(): ReminderDao
    abstract fun smsImportDao(): SMSImportDao

    companion object {
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE loans ADD COLUMN interestType TEXT NOT NULL DEFAULT 'REDUCING'"
                )
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE loans ADD COLUMN emiDueDay INTEGER NOT NULL DEFAULT 1"
                )
            }
        }
    }
}
