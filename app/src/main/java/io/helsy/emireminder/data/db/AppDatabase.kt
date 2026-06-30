package io.helsy.emireminder.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import io.helsy.emireminder.data.db.dao.LoanDao
import io.helsy.emireminder.data.db.dao.ReminderDao
import io.helsy.emireminder.data.db.dao.SMSImportDao
import io.helsy.emireminder.data.db.entity.Loan
import io.helsy.emireminder.data.db.entity.Reminder
import io.helsy.emireminder.data.db.entity.SMSImport

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
