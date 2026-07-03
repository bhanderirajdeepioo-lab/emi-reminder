package com.emireminder.app.sms

import android.content.Context
import android.net.Uri
import com.emireminder.app.data.repository.SmsFinanceRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject
import javax.inject.Singleton

data class ScanProgress(val processed: Int, val inserted: Int)

private val SMS_INBOX_URI: Uri = Uri.parse("content://sms/inbox")
private const val SIX_MONTHS_MS = 183L * 24 * 60 * 60 * 1000
private const val PROGRESS_EMIT_INTERVAL = 50

@Singleton
class HistoricalSmsScanner @Inject constructor(
    @ApplicationContext private val context: Context,
    private val smsFinanceRepository: SmsFinanceRepository,
) {
    /**
     * Queries the SMS inbox for the last 6 months, parses every message, and inserts
     * import-eligible transactions (confidence > 50) into Room.
     *
     * The unique `_id` from the ContentProvider is used as `smsId` so re-scans
     * skip already-imported messages via the UNIQUE index on parsed_transactions.sms_id.
     *
     * Emits [ScanProgress] every [PROGRESS_EMIT_INTERVAL] messages and once at the end.
     */
    fun scan(): Flow<ScanProgress> = flow {
        val cutoffMs = System.currentTimeMillis() - SIX_MONTHS_MS

        val cursor = context.contentResolver.query(
            SMS_INBOX_URI,
            arrayOf("_id", "address", "body", "date"),
            "date >= ?",
            arrayOf(cutoffMs.toString()),
            "date DESC",
        ) ?: return@flow

        var processed = 0
        var inserted = 0

        cursor.use { c ->
            val idIdx   = c.getColumnIndexOrThrow("_id")
            val addrIdx = c.getColumnIndexOrThrow("address")
            val bodyIdx = c.getColumnIndexOrThrow("body")
            val dateIdx = c.getColumnIndexOrThrow("date")

            while (c.moveToNext()) {
                val smsId   = c.getLong(idIdx).toString()
                val address = c.getString(addrIdx) ?: continue
                val body    = c.getString(bodyIdx) ?: continue
                val dateMs  = c.getLong(dateIdx)

                processed++

                if (smsFinanceRepository.processHistoricalSms(smsId, address, body, dateMs)) {
                    inserted++
                }

                if (processed % PROGRESS_EMIT_INTERVAL == 0) {
                    emit(ScanProgress(processed, inserted))
                }
            }
        }

        emit(ScanProgress(processed, inserted))
    }.flowOn(Dispatchers.IO)
}
