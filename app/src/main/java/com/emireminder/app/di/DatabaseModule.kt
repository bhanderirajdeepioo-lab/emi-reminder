package com.emireminder.app.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import com.emireminder.app.data.db.AppDatabase
import com.emireminder.app.data.db.dao.LoanDao
import com.emireminder.app.data.db.dao.ReminderDao
import com.emireminder.app.data.db.dao.SMSImportDao
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    private val seedCallback = object : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            // Insert one realistic demo loan so new users see a populated dashboard.
            db.execSQL(
                """INSERT INTO loans
                   (name, type, principalAmount, interestRate, tenureMonths, emiAmount,
                    startDate, isActive, notes, bankName, accountNumber, interestType, emiDueDay)
                   VALUES
                   ('Personal Loan ICICI', 'PERSONAL', 300000.0, 12.5, 36, 10056.0,
                    ${System.currentTimeMillis()}, 1, '', 'ICICI Bank', '', 'REDUCING', 5)"""
            )
            // Seed a matching reminder for the demo loan (loanId = 1 from autoGenerate).
            db.execSQL(
                """INSERT INTO reminders
                   (loanId, loanName, bankName, emiAmount, dueDayOfMonth,
                    frequency, isActive, notificationEnabled, notes)
                   VALUES
                   (1, 'Personal Loan ICICI', 'ICICI Bank', 10056.0, 5,
                    'MONTHLY', 1, 1, '')"""
            )
        }
    }

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "emi_reminder.db"
        )
        .addMigrations(AppDatabase.MIGRATION_2_3, AppDatabase.MIGRATION_3_4)
        .addCallback(seedCallback)
        .fallbackToDestructiveMigration()
        .fallbackToDestructiveMigrationOnDowngrade()
        .build()

    @Provides
    @Singleton
    fun provideLoanDao(db: AppDatabase): LoanDao = db.loanDao()

    @Provides
    @Singleton
    fun provideReminderDao(db: AppDatabase): ReminderDao = db.reminderDao()

    @Provides
    @Singleton
    fun provideSmsImportDao(db: AppDatabase): SMSImportDao = db.smsImportDao()
}
