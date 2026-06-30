package io.helsy.emireminder.sms

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Reads the device SMS inbox via ContentResolver and returns parsed EMI candidates.
 * READ_SMS permission must be granted before calling [scan].
 * All data stays 100% on-device — no network calls.
 */
@Singleton
class SmsScanner @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val inboxUri: Uri = Uri.parse("content://sms/inbox")

    suspend fun scan(maxMessages: Int = 500): List<SmsParseResult> = withContext(Dispatchers.IO) {
        val results = mutableListOf<SmsParseResult>()
        val resolver: ContentResolver = context.contentResolver

        val cursor = resolver.query(
            inboxUri,
            arrayOf("address", "body"),
            null, null,
            "date DESC LIMIT $maxMessages",
        ) ?: return@withContext results

        cursor.use {
            val addrIdx = it.getColumnIndex("address")
            val bodyIdx = it.getColumnIndex("body")
            while (it.moveToNext()) {
                val address = it.getString(addrIdx) ?: continue
                val body    = it.getString(bodyIdx) ?: continue
                val result  = SmsParser.parse(address, body)
                if (result.confidence >= 0.4f) {
                    results += result
                }
            }
        }
        results
    }
}
