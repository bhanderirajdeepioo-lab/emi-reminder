package com.emireminder.app.data.repository

import com.emireminder.app.data.db.dao.ReminderDao
import com.emireminder.app.data.db.dao.SMSImportDao
import com.emireminder.app.data.db.entity.Reminder
import com.emireminder.app.data.db.entity.SMSImport
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReminderRepository @Inject constructor(
    private val reminderDao: ReminderDao,
    private val smsImportDao: SMSImportDao,
) {
    fun getAllReminders(): Flow<List<Reminder>> = reminderDao.getAllReminders()

    fun getActiveReminders(): Flow<List<Reminder>> = reminderDao.getActiveReminders()

    suspend fun getReminderById(id: Int): Reminder? = reminderDao.getReminderById(id)

    fun getRemindersByLoan(loanId: Int): Flow<List<Reminder>> = reminderDao.getRemindersByLoan(loanId)

    suspend fun insertReminder(reminder: Reminder): Long = reminderDao.insertReminder(reminder)

    suspend fun updateReminder(reminder: Reminder) = reminderDao.updateReminder(reminder)

    suspend fun deleteReminder(reminder: Reminder) = reminderDao.deleteReminder(reminder)

    fun getPendingSmsImports(): Flow<List<SMSImport>> = smsImportDao.getPendingSmsImports()

    suspend fun insertSmsImport(smsImport: SMSImport): Long = smsImportDao.insertSmsImport(smsImport)

    suspend fun confirmSmsImport(smsImport: SMSImport, linkedLoanId: Int) =
        smsImportDao.updateSmsImport(smsImport.copy(isConfirmed = true, linkedLoanId = linkedLoanId))
}
