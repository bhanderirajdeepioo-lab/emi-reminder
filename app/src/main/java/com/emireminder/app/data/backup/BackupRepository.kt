package com.emireminder.app.data.backup

import android.content.Context
import android.net.Uri
import com.emireminder.app.data.db.entity.Loan
import com.emireminder.app.data.db.entity.Reminder
import com.emireminder.app.data.repository.LoanRepository
import com.emireminder.app.data.repository.ReminderRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Backup file format (JSON v1):
 * {
 *   "version": 1,
 *   "exportedAt": <epoch ms>,
 *   "loans": [ { id, name, type, principalAmount, interestRate, tenureMonths, emiAmount,
 *                startDate, isActive, notes, bankName, accountNumber, interestType,
 *                emiDueDay }, ... ],
 *   "reminders": [ { id, loanId, loanName, bankName, emiAmount, dueDayOfMonth,
 *                    frequency, isActive, notificationEnabled, notes, lastTriggeredAt, upiVpa }, ... ]
 * }
 * Restore uses upsert (OnConflictStrategy.REPLACE on primary key) so re-running restore
 * on an already-populated DB produces no duplicates.
 */
@Singleton
class BackupRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val loanRepository: LoanRepository,
    private val reminderRepository: ReminderRepository,
) {

    suspend fun backup(uri: Uri) = withContext(Dispatchers.IO) {
        val loans = loanRepository.getAllLoans().first()
        val reminders = reminderRepository.getAllReminders().first()

        val json = JSONObject().apply {
            put("version", 1)
            put("exportedAt", System.currentTimeMillis())
            put("loans", JSONArray().also { arr -> loans.forEach { arr.put(loanToJson(it)) } })
            put("reminders", JSONArray().also { arr -> reminders.forEach { arr.put(reminderToJson(it)) } })
        }

        context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { writer ->
            writer.write(json.toString(2))
        } ?: error("Could not open output stream for URI")
    }

    suspend fun restore(uri: Uri): RestoreResult = withContext(Dispatchers.IO) {
        val raw = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            ?: return@withContext RestoreResult.Error("Could not read backup file")

        try {
            val json = JSONObject(raw)
            val version = json.optInt("version", 1)
            if (version > 1) return@withContext RestoreResult.Error("Unsupported backup version: $version")

            val loansArray = json.getJSONArray("loans")
            val remindersArray = json.getJSONArray("reminders")

            var loanCount = 0
            for (i in 0 until loansArray.length()) {
                val loan = jsonToLoan(loansArray.getJSONObject(i))
                loanRepository.insertLoan(loan)
                loanCount++
            }

            var reminderCount = 0
            for (i in 0 until remindersArray.length()) {
                val reminder = jsonToReminder(remindersArray.getJSONObject(i))
                reminderRepository.insertReminder(reminder)
                reminderCount++
            }

            RestoreResult.Success(loanCount, reminderCount)
        } catch (e: Exception) {
            RestoreResult.Error("Invalid backup file: ${e.message}")
        }
    }

    private fun loanToJson(loan: Loan) = JSONObject().apply {
        put("id", loan.id)
        put("name", loan.name)
        put("type", loan.type)
        put("principalAmount", loan.principalAmount)
        put("interestRate", loan.interestRate)
        put("tenureMonths", loan.tenureMonths)
        put("emiAmount", loan.emiAmount)
        put("startDate", loan.startDate)
        put("isActive", loan.isActive)
        put("notes", loan.notes)
        put("bankName", loan.bankName)
        put("accountNumber", loan.accountNumber)
        put("interestType", loan.interestType)
        put("emiDueDay", loan.emiDueDay)
    }

    private fun jsonToLoan(o: JSONObject) = Loan(
        id = o.getInt("id"),
        name = o.getString("name"),
        type = o.getString("type"),
        principalAmount = o.getDouble("principalAmount"),
        interestRate = o.getDouble("interestRate"),
        tenureMonths = o.getInt("tenureMonths"),
        emiAmount = o.getDouble("emiAmount"),
        startDate = o.getLong("startDate"),
        isActive = o.getBoolean("isActive"),
        notes = o.optString("notes", ""),
        bankName = o.optString("bankName", ""),
        accountNumber = o.optString("accountNumber", ""),
        interestType = o.optString("interestType", "REDUCING"),
        emiDueDay = o.optInt("emiDueDay", 1),
    )

    private fun reminderToJson(r: Reminder) = JSONObject().apply {
        put("id", r.id)
        if (r.loanId != null) put("loanId", r.loanId) else put("loanId", JSONObject.NULL)
        put("loanName", r.loanName)
        put("bankName", r.bankName)
        put("emiAmount", r.emiAmount)
        put("dueDayOfMonth", r.dueDayOfMonth)
        put("frequency", r.frequency)
        put("isActive", r.isActive)
        put("notificationEnabled", r.notificationEnabled)
        put("notes", r.notes)
        if (r.lastTriggeredAt != null) put("lastTriggeredAt", r.lastTriggeredAt) else put("lastTriggeredAt", JSONObject.NULL)
        put("upiVpa", r.upiVpa)
    }

    private fun jsonToReminder(o: JSONObject) = Reminder(
        id = o.getInt("id"),
        loanId = if (o.isNull("loanId")) null else o.getInt("loanId"),
        loanName = o.getString("loanName"),
        bankName = o.optString("bankName", ""),
        emiAmount = o.getDouble("emiAmount"),
        dueDayOfMonth = o.getInt("dueDayOfMonth"),
        frequency = o.optString("frequency", "MONTHLY"),
        isActive = o.getBoolean("isActive"),
        notificationEnabled = o.optBoolean("notificationEnabled", true),
        notes = o.optString("notes", ""),
        lastTriggeredAt = if (o.isNull("lastTriggeredAt")) null else o.getLong("lastTriggeredAt"),
        upiVpa = o.optString("upiVpa", ""),
    )
}

sealed interface RestoreResult {
    data class Success(val loanCount: Int, val reminderCount: Int) : RestoreResult
    data class Error(val message: String) : RestoreResult
}
