package com.emireminder.app.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.emireminder.app.data.db.dao.LoanDao
import com.emireminder.app.data.db.dao.ReminderDao
import com.emireminder.app.data.db.dao.SMSImportDao
import com.emireminder.app.data.db.entity.Loan
import com.emireminder.app.data.db.entity.Reminder
import com.emireminder.app.data.db.entity.SMSImport

@Database(
    entities = [Loan::class, Reminder::class, SMSImport::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun loanDao(): LoanDao
    abstract fun reminderDao(): ReminderDao
    abstract fun smsImportDao(): SMSImportDao
}
