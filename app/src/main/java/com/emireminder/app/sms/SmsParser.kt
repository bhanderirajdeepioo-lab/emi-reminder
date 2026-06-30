package com.emireminder.app.sms

/**
 * Pure-Kotlin SMS parser — zero Android imports so it can run in JVM unit tests.
 *
 * Confidence tiers:
 *   >= 0.8  → auto-import eligible
 *   0.4–0.8 → surface for user confirmation
 *   < 0.4   → skip
 */
object SmsParser {

    // Matches Rs.3,500.00 / Rs 5000 / INR 12,500 / ₹ 9,000.00
    private val AMOUNT_RE = Regex("""(?:Rs\.?\s*|INR\s*|₹\s*)([\d,]+(?:\.\d{1,2})?)""")

    // Last 3–6 digits of account / loan number (preceded by XX masking)
    private val ACCOUNT_RE = Regex(
        """(?:a/c|acct|account|loan\s+a/c|loan\s+no\.?)[\s:]*(?:[Xx*]+)?(\d{3,6})""",
        RegexOption.IGNORE_CASE,
    )

    // Debit/credit action keywords — order-independent check
    private val ACTION_RE = Regex("""(?i)(?:debited?|paid|process(?:ed)?|credited?|deducted?|auto.?debit)""")

    // ──── HDFC ────
    private val HDFC_SENDER = Regex("""(?i)HDFC|HDFCBK|HDFCBN|HDFCBANKL""")
    private val HDFC_BODY   = Regex("""(?i)HDFC\s*Bank""")
    private val HDFC_EMI    = Regex("""(?i)(?:EMI|equated\s+monthly)""")

    // ──── SBI ────
    private val SBI_SENDER = Regex("""(?i)\bSBI\b|SBIINB|SBICRD|SBIMSG|SBICARD""")
    private val SBI_BODY   = Regex("""(?i)(?:State\s+Bank\s+of\s+India|SBICARD|SBI\s+(?:Bank|Loan|Credit))""")
    private val SBI_EMI    = Regex("""(?i)(?:EMI|loan\s+instalment|instalment|installment|auto\s+debit)""")

    // ──── ICICI ────
    private val ICICI_SENDER = Regex("""(?i)ICICI|ICICIB|ICICIS|ICINB""")
    private val ICICI_BODY   = Regex("""(?i)ICICI\s*(?:Bank|Lombard|Pru)""")
    private val ICICI_EMI    = Regex("""(?i)(?:auto.?debit|EMI|deducted|equated\s+monthly)""")

    // ──── Generic fallback ────
    private val GENERIC_EMI = Regex("""(?i)(?:\bemi\b|loan\s+(?:emi|instalment|installment|payment)|equated\s+monthly)""")

    fun parse(senderAddress: String, body: String): SmsParseResult {
        val amount = extractAmount(body)

        return when {
            HDFC_SENDER.containsMatchIn(senderAddress) || HDFC_BODY.containsMatchIn(body) ->
                bankResult(BankSource.HDFC, amount, body, HDFC_EMI)

            SBI_SENDER.containsMatchIn(senderAddress) || SBI_BODY.containsMatchIn(body) ->
                bankResult(BankSource.SBI, amount, body, SBI_EMI)

            ICICI_SENDER.containsMatchIn(senderAddress) || ICICI_BODY.containsMatchIn(body) ->
                bankResult(BankSource.ICICI, amount, body, ICICI_EMI)

            amount != null && GENERIC_EMI.containsMatchIn(body) ->
                SmsParseResult(BankSource.GENERIC, amount, extractAccount(body), 0.65f, body)

            else ->
                SmsParseResult(BankSource.UNKNOWN, 0.0, "", 0.1f, body)
        }
    }

    private fun bankResult(
        bank: BankSource,
        amount: Double?,
        body: String,
        emiPattern: Regex,
    ): SmsParseResult {
        if (amount == null) return SmsParseResult(bank, 0.0, "", 0.3f, body)
        // High confidence requires BOTH an EMI keyword AND a transaction action — order doesn't matter
        val confidence = if (emiPattern.containsMatchIn(body) && ACTION_RE.containsMatchIn(body)) 0.9f else 0.65f
        return SmsParseResult(bank, amount, extractAccount(body), confidence, body)
    }

    private fun extractAmount(body: String): Double? =
        AMOUNT_RE.find(body)?.groupValues?.get(1)?.replace(",", "")?.toDoubleOrNull()

    private fun extractAccount(body: String): String =
        ACCOUNT_RE.find(body)?.groupValues?.getOrNull(1) ?: ""
}
