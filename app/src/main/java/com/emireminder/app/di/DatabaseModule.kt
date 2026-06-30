package com.emireminder.app.di

import android.content.Context
import androidx.room.Room
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

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "emi_reminder.db"
        )
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
