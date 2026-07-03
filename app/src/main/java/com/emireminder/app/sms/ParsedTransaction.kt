package com.emireminder.app.sms

/**
 * Structured output of [SmsParser.parseTransaction].
 *
 * confidenceScore — 0–100 scale:
 *   >= 50 → import-eligible
 *   < 50  → analytics-only (surface via logging, never create user-visible reminders)
 */
data class ParsedTransaction(
    val category: TransactionCategory,
    val amount: Double,
    val date: String?,
    val accountLast4: String?,
    val vpa: String?,
    val merchantName: String?,
    val senderAddress: String,
    val confidenceScore: Int,
    val rawBody: String,
) {
    val isImportEligible: Boolean get() = confidenceScore >= 50
}
