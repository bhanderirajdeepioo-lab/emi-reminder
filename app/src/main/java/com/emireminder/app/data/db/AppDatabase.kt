package com.emireminder.app.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.emireminder.app.data.db.converter.SmsFinanceConverters
import com.emireminder.app.data.db.dao.AutoDetectedEmiDao
import com.emireminder.app.data.db.dao.BankAccountDao
import com.emireminder.app.data.db.dao.LoanDao
import com.emireminder.app.data.db.dao.MonthlyFinanceSummaryDao
import com.emireminder.app.data.db.dao.ParsedTransactionDao
import com.emireminder.app.data.db.dao.ReminderDao
import com.emireminder.app.data.db.dao.SMSImportDao
import com.emireminder.app.data.db.entity.AutoDetectedEmi
import com.emireminder.app.data.db.entity.BankAccount
import com.emireminder.app.data.db.entity.Loan
import com.emireminder.app.data.db.entity.MonthlyFinanceSummary
import com.emireminder.app.data.db.entity.ParsedTransaction
import com.emireminder.app.data.db.entity.Reminder
import com.emireminder.app.data.db.entity.SMSImport

@Database(
    entities = [
        Loan::class,
        Reminder::class,
        SMSImport::class,
        BankAccount::class,
        ParsedTransaction::class,
        AutoDetectedEmi::class,
        MonthlyFinanceSummary::class,
    ],
    version = 7,
    exportSchema = false
)
@TypeConverters(SmsFinanceConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun loanDao(): LoanDao
    abstract fun reminderDao(): ReminderDao
    abstract fun smsImportDao(): SMSImportDao
    abstract fun bankAccountDao(): BankAccountDao
    abstract fun parsedTransactionDao(): ParsedTransactionDao
    abstract fun autoDetectedEmiDao(): AutoDetectedEmiDao
    abstract fun monthlyFinanceSummaryDao(): MonthlyFinanceSummaryDao

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

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE loans ADD COLUMN upiVpa TEXT NOT NULL DEFAULT ''"
                )
                database.execSQL(
                    "ALTER TABLE reminders ADD COLUMN upiVpa TEXT NOT NULL DEFAULT ''"
                )
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // upiVpa already added in MIGRATION_4_5; no schema changes in v6.
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `bank_accounts` (
                        `id` TEXT NOT NULL,
                        `sender_id` TEXT NOT NULL,
                        `bank_name` TEXT NOT NULL,
                        `account_last4` TEXT NOT NULL,
                        `account_label` TEXT,
                        `label_confirmed` INTEGER NOT NULL DEFAULT 0,
                        `first_seen_at` INTEGER NOT NULL,
                        `label_prompted_at` INTEGER,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                database.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_bank_accounts_sender_id_account_last4` ON `bank_accounts` (`sender_id`, `account_last4`)"
                )

                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `parsed_transactions` (
                        `id` TEXT NOT NULL,
                        `sms_id` TEXT NOT NULL,
                        `raw_sms_body` TEXT,
                        `parsed_at` INTEGER NOT NULL,
                        `transaction_date` INTEGER NOT NULL,
                        `year_month` TEXT NOT NULL,
                        `amount` REAL NOT NULL,
                        `currency` TEXT NOT NULL DEFAULT 'INR',
                        `direction` TEXT NOT NULL,
                        `category` TEXT NOT NULL,
                        `sub_category` TEXT,
                        `sender_id` TEXT NOT NULL,
                        `bank_name` TEXT NOT NULL,
                        `account_last4` TEXT NOT NULL,
                        `bank_account_id` TEXT NOT NULL,
                        `merchant_name` TEXT,
                        `vpa` TEXT,
                        `utr_ref` TEXT,
                        `confidence_score` INTEGER NOT NULL,
                        `is_emi` INTEGER NOT NULL DEFAULT 0,
                        `loan_account_last4` TEXT,
                        `user_verified` INTEGER NOT NULL DEFAULT 0,
                        `notes` TEXT,
                        PRIMARY KEY(`id`),
                        FOREIGN KEY(`bank_account_id`) REFERENCES `bank_accounts`(`id`) ON DELETE RESTRICT
                    )
                    """.trimIndent()
                )
                database.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_parsed_transactions_sms_id` ON `parsed_transactions` (`sms_id`)"
                )
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_parsed_transactions_bank_account_id` ON `parsed_transactions` (`bank_account_id`)"
                )
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_parsed_transactions_year_month` ON `parsed_transactions` (`year_month`)"
                )
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_parsed_transactions_category` ON `parsed_transactions` (`category`)"
                )

                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `auto_detected_emis` (
                        `id` TEXT NOT NULL,
                        `transaction_id` TEXT NOT NULL,
                        `lender_name` TEXT NOT NULL,
                        `loan_account_last4` TEXT NOT NULL,
                        `emi_amount` REAL NOT NULL,
                        `detected_date` INTEGER NOT NULL,
                        `recurring_day` INTEGER NOT NULL,
                        `linked_reminder_id` INTEGER,
                        `status` TEXT NOT NULL,
                        `confirmation_at` INTEGER,
                        PRIMARY KEY(`id`),
                        FOREIGN KEY(`transaction_id`) REFERENCES `parsed_transactions`(`id`) ON DELETE CASCADE,
                        FOREIGN KEY(`linked_reminder_id`) REFERENCES `reminders`(`id`) ON DELETE SET NULL
                    )
                    """.trimIndent()
                )
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_auto_detected_emis_transaction_id` ON `auto_detected_emis` (`transaction_id`)"
                )
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_auto_detected_emis_linked_reminder_id` ON `auto_detected_emis` (`linked_reminder_id`)"
                )

                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `monthly_finance_summaries` (
                        `year_month` TEXT NOT NULL,
                        `total_income` REAL NOT NULL,
                        `total_emi` REAL NOT NULL,
                        `total_expenses` REAL NOT NULL,
                        `top_categories` TEXT NOT NULL,
                        `net_savings` REAL NOT NULL,
                        `created_at` INTEGER NOT NULL,
                        PRIMARY KEY(`year_month`)
                    )
                    """.trimIndent()
                )
            }
        }
    }
}
