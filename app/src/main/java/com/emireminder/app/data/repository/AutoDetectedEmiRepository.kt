package com.emireminder.app.data.repository

import com.emireminder.app.data.db.dao.AutoDetectedEmiDao
import com.emireminder.app.data.db.entity.AutoDetectedEmi
import com.emireminder.app.data.db.entity.ParsedTransaction
import com.emireminder.app.domain.model.EmiStatus
import kotlinx.coroutines.flow.Flow
import java.util.Calendar
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

@Singleton
class AutoDetectedEmiRepository @Inject constructor(
    private val dao: AutoDetectedEmiDao,
) {

    fun getPendingEmis(): Flow<List<AutoDetectedEmi>> =
        dao.getByStatus(EmiStatus.PENDING_CONFIRM)

    /**
     * Called after storing a ParsedTransaction where isEmi = true.
     * Returns the AutoDetectedEmi if a notification card should surface; null if silently ignored.
     *
     * AC #7: amount change ≤ ₹10 → silent, no card shown.
     * Deduplication: if a PENDING_CONFIRM already exists for same lender+account, skip.
     */
    suspend fun processEmiTransaction(transaction: ParsedTransaction): AutoDetectedEmi? {
        val lenderName = transaction.bankName.ifBlank { transaction.senderId }
        val loanLast4 = transaction.loanAccountLast4.orEmpty()

        if (dao.findPendingByLenderAndAccount(lenderName, loanLast4) != null) return null

        val confirmed = dao.findConfirmedByLenderAndAccount(lenderName, loanLast4)
        if (confirmed != null) {
            val amountDiff = abs(transaction.amount - confirmed.emiAmount)
            if (amountDiff <= 10.0) return null
        }

        val recurringDay = inferRecurringDay(lenderName, loanLast4, transaction.transactionDate)
        val emi = AutoDetectedEmi(
            id = UUID.randomUUID().toString(),
            transactionId = transaction.id,
            lenderName = lenderName,
            loanAccountLast4 = loanLast4,
            emiAmount = transaction.amount,
            detectedDate = System.currentTimeMillis(),
            recurringDay = recurringDay,
            status = EmiStatus.PENDING_CONFIRM,
        )
        dao.insert(emi)
        return emi
    }

    suspend fun dismissEmi(id: String) {
        val emi = dao.getById(id) ?: return
        dao.update(emi.copy(status = EmiStatus.DISMISSED))
    }

    suspend fun confirmEmi(id: String, reminderId: Int) {
        val emi = dao.getById(id) ?: return
        dao.update(
            emi.copy(
                status = EmiStatus.CONFIRMED,
                linkedReminderId = reminderId,
                confirmationAt = System.currentTimeMillis(),
            )
        )
    }

    suspend fun getPreviousConfirmedAmount(lenderName: String, loanAccountLast4: String): Double? =
        dao.findConfirmedByLenderAndAccount(lenderName, loanAccountLast4)?.emiAmount

    suspend fun deleteAll() = dao.deleteAll()

    /**
     * Infer the most likely monthly debit day using median of up to 2 historical detections + current.
     * PRD §8.2: take median day across last 3 matching SMS.
     */
    private suspend fun inferRecurringDay(
        lenderName: String,
        loanAccountLast4: String,
        transactionDate: Long,
    ): Int {
        val currentDay = calendarDay(transactionDate)
        val previousDates = dao.getRecentDetectionDates(lenderName, loanAccountLast4, 2)
        if (previousDates.isEmpty()) return currentDay
        val days = (previousDates.map { calendarDay(it) } + currentDay).sorted()
        return days[days.size / 2]
    }

    private fun calendarDay(epochMillis: Long): Int =
        Calendar.getInstance().apply { timeInMillis = epochMillis }.get(Calendar.DAY_OF_MONTH)
}
