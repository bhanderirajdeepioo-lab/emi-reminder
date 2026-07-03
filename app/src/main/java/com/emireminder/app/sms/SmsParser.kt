package com.emireminder.app.sms

/**
 * Pure-Kotlin SMS parser — zero Android imports so it can run in JVM unit tests.
 *
 * Confidence tiers (legacy parse() float scale):
 *   >= 0.8  → auto-import eligible
 *   0.4–0.8 → surface for user confirmation
 *   < 0.4   → skip
 *
 * Confidence tiers (parseTransaction() integer 0–100 scale per PRD §5.4):
 *   >= 50   → import-eligible
 *   < 50    → analytics-only, silently skip import
 *   null    → not created at all (OTP, promo, unknown sender)
 */
object SmsParser {

    // ─────────────────────────────────────────────────────────────────────────
    // LEGACY parse() — backward-compatible, EMI-focused
    // ─────────────────────────────────────────────────────────────────────────

    private val AMOUNT_RE = Regex("""(?:Rs\.?\s*|INR\s*|₹\s*)([\d,]+(?:\.\d{1,2})?)""")

    private val ACCOUNT_RE = Regex(
        """(?:a/c|acct|account|loan\s+a/c|loan\s+no\.?)[\s:]*(?:[Xx*]+)?(\d{3,6})""",
        RegexOption.IGNORE_CASE,
    )

    private val ACTION_RE = Regex("""(?i)(?:debited?|paid|process(?:ed)?|credited?|deducted?|auto.?debit)""")

    private val HDFC_SENDER = Regex("""(?i)HDFC|HDFCBK|HDFCBN|HDFCBANKL""")
    private val HDFC_BODY   = Regex("""(?i)HDFC\s*Bank""")
    private val HDFC_EMI    = Regex("""(?i)(?:EMI|equated\s+monthly)""")

    private val SBI_SENDER = Regex("""(?i)\bSBI\b|SBIINB|SBICRD|SBIMSG|SBICARD""")
    private val SBI_BODY   = Regex("""(?i)(?:State\s+Bank\s+of\s+India|SBICARD|SBI\s+(?:Bank|Loan|Credit))""")
    private val SBI_EMI    = Regex("""(?i)(?:EMI|loan\s+instalment|instalment|installment|auto\s+debit)""")

    private val ICICI_SENDER = Regex("""(?i)ICICI|ICICIB|ICICIS|ICINB""")
    private val ICICI_BODY   = Regex("""(?i)ICICI\s*(?:Bank|Lombard|Pru)""")
    private val ICICI_EMI    = Regex("""(?i)(?:auto.?debit|EMI|deducted|equated\s+monthly)""")

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
        val confidence = if (emiPattern.containsMatchIn(body) && ACTION_RE.containsMatchIn(body)) 0.9f else 0.65f
        return SmsParseResult(bank, amount, extractAccount(body), confidence, body)
    }

    private fun extractAmount(body: String): Double? =
        AMOUNT_RE.find(body)?.groupValues?.get(1)?.replace(",", "")?.toDoubleOrNull()

    private fun extractAccount(body: String): String =
        ACCOUNT_RE.find(body)?.groupValues?.getOrNull(1) ?: ""

    // ─────────────────────────────────────────────────────────────────────────
    // NEW parseTransaction() — full PRD §5 finance intelligence engine
    // ─────────────────────────────────────────────────────────────────────────

    // ── Sender whitelist (DLT-registered Indian bank sender IDs) ─────────────

    private val KNOWN_SENDER_IDS = setOf(
        // HDFC Bank
        "HDFCBK", "HDFCBANKL", "HDFCBN", "HDFCBANKCC",
        // State Bank of India
        "SBISMS", "SBIINB", "SBICRD", "SBIMSG", "SBICARD",
        // ICICI Bank
        "ICICIB", "ICICIS", "ICINB", "ICICIBK",
        // Axis Bank
        "AXISBK", "AXISBANKL", "AXISBNK", "UTIBOP",
        // Kotak Mahindra Bank
        "KOTAKB", "KOTAKM",
        // Punjab National Bank
        "PNBSMS", "PNBMOB",
        // Bank of India
        "BOIIND",
        // Canara Bank
        "CANBNK", "CANARABNK",
        // UCO Bank
        "UCOBNK",
        // IDBI Bank
        "IDBIBNK", "IDBIBANKL",
        // Yes Bank
        "YESBNK", "YESBANK",
        // IndusInd Bank
        "INDUSB", "INDUSLND",
        // Bank of Baroda
        "BARODASMS", "BOBIBNK",
        // Central Bank of India
        "CENTBNK",
        // Indian Bank
        "INDBNK",
        // Paytm Payments Bank
        "PAYTMB",
        // Federal Bank
        "FEDBK", "FEDBNK",
        // IDFC First Bank
        "IDFCFB", "IDFCFST",
        // RBL Bank
        "RBLBNK",
        // AU Small Finance Bank
        "AUBANK",
        // Bajaj Finserv / Bajaj Finance
        "BAJAJF", "BAJFINSERV", "BJFINSERV", "BAJAJFIN",
        // Muthoot Finance
        "MUTHOOT", "MUTHOOTF", "MUTHOOTFIN",
        // Manappuram Finance
        "MANAPL", "MANAPPURAM", "MANAPM",
        // HDB Financial Services
        "HDBFSL", "HDBFIN",
        // Tata Capital
        "TATACAP", "TATACAPF", "TATAFIN",
        // L&T Finance
        "LNTFIN", "LTFINANCE", "LNTFSL",
        // Slice
        "SLICEIT", "SLICEPAY",
        // BharatPe
        "BHARATPE", "BHRTPE",
        // Navi Finance
        "NAVIFIN", "NAVIFSV",
        // KreditBee
        "KREDITB", "KREDITBEE",
        // Piramal Finance
        "PIRAMALF", "PIRAMALC",
        // DMI Finance
        "DMIFIN",
        // Aditya Birla Finance
        "ABFSSL", "ABFINANCE",
        // Shriram Finance
        "SHRIRAM", "SHRIRAMF",
        // Fullerton India
        "FULLERTONIN", "FULLRTN",
        // LIC of India
        "LICIND", "LICOFI", "LICPOL",
        // HDFC Life Insurance
        "HDFCLI", "HDFCLIFE",
        // ICICI Prudential Life
        "ICICIP", "ICICIPRU",
        // SBI Life Insurance
        "SBILI", "SBILIFC", "SBILIFE",
        // Bajaj Allianz
        "BAJALZ", "BAJAJAL", "BJALZ",
        // Tata AIA
        "TATAAI", "TATAAIG", "TATAAIA",
        // Max Life Insurance
        "MAXLIF", "MAXLIFE",
        // Kotak Life Insurance
        "KOTAKL", "KOTAKLIF",
        // Star Health Insurance
        "STRHLT", "STARHLTH",
        // HDFC ERGO
        "HDFCER", "HDFCERG",
        // ICICI Lombard
        "ICICILG", "ICINLBR",
        // New India Assurance
        "NEWIND", "NIAIND",
        // United India Insurance
        "UNIIND",
        // Reliance General Insurance
        "RELGEN", "RELGINS",
        // Jio
        "JIOIND", "JIOMSG", "RELJI",
        // Airtel
        "AIRTEL", "BRTL", "AIRINB",
        // Vodafone Idea / Vi
        "VFIN", "VIIND", "VIDEOC",
        // BSNL
        "BSNLSM", "BSNLIN",
        // Tata Play
        "TATPLY", "TATAPL",
        // Dish TV / Hathway
        "DISHSM", "DISHTV", "HTHWAY",
    )

    // Matches the bank portion of a DLT sender ID like "AX-HDFCBK" or bare "HDFCBK"
    private val SENDER_BANK_KEYWORD_RE = Regex(
        """(?i)(?:HDFC|SBI|ICICI|AXIS|KOTAK|PNB|BOI|CANARA|UCO|IDBI|YES|INDUSLND|BARODA|CENTB|PAYTMB|FEDERAL|IDFCFB|RBL|AUBANK|INDUS|INDBK|BAJAJ|MUTHOOT|MANAPL|HDBFSL|TATACAP|LNTFIN|SLICE|BHARATPE|NAVIFIN|KREDITB|PIRAMALF|DMIFIN|ABFSSL|SHRIRAM|FULLRTN)"""
    )

    // Bank/NBFC name mentions in SMS body (fallback when sender ID isn't whitelisted)
    private val BODY_BANK_RE = Regex(
        """(?i)(?:HDFC\s*Bank|State\s*Bank\s*of\s*India|ICICI\s*Bank|Axis\s*Bank|Kotak\s*(?:Mahindra\s*)?Bank|Punjab\s*National\s*Bank|Bank\s*of\s*India|Canara\s*Bank|UCO\s*Bank|IDBI\s*Bank|Yes\s*Bank|IndusInd\s*Bank|Bank\s*of\s*Baroda|IDFC\s*First\s*Bank|Federal\s*Bank|RBL\s*Bank|Central\s*Bank\s*of\s*India|Paytm\s*(?:Payments\s*)?Bank|Bajaj\s*Finserv|Bajaj\s*Finance|Muthoot\s*Finance|Manappuram\s*Finance|HDB\s*Financial|Tata\s*Capital|L\s*&\s*T\s*Finance|Slice|BharatPe|Navi|KreditBee|Piramal\s*(?:Capital|Finance)|DMI\s*Finance|Aditya\s*Birla\s*(?:Finance|Capital)|Shriram\s*Finance|Fullerton\s*India|LIC\s*(?:of\s*India)?|HDFC\s*Life|ICICI\s*(?:Prudential|Pru)|SBI\s*Life|Bajaj\s*Allianz|Tata\s*AIA|Max\s*Life|Kotak\s*Life|Star\s*Health|HDFC\s*ERGO|ICICI\s*Lombard|Reliance\s*(?:General|Life)\s*Insurance|Jio|Airtel|Vodafone\s*Idea|Vi\s*Mobile|BSNL|Tata\s*Play|Dish\s*TV)"""
    )

    // ── OTP / promo filters ───────────────────────────────────────────────────

    private val OTP_RE = Regex(
        """(?i)(?:\botp\b|one.time.(?:password|pin|code)|verification.?(?:code|pin|otp)|do\s+not\s+share\s+(?:this\s+)?(?:otp|code|pin))"""
    )

    private val PROMO_RE = Regex(
        """(?i)(?:\boffer\b|\bdeal\b|\bdiscount\b|\bcashback\b|\bvoucher\b|\bprize\b|\bwon\b|\bwinner\b|\blottery\b|\bclick\s+here\b|\bcongratulations\b|\bexclusive\b)"""
    )

    private val FINANCIAL_KEYWORD_RE = Regex(
        """(?i)(?:debited?|credited?|paid|withdrawn?|deducted?|processed?|transferred?|received|deposited?|instalment|installment|salary|emi|neft|imps|rtgs|upi|premium|recharge|subscription|renewal|bill\s+paid|due\s+paid|mandated?|auto.?pay|standing\s+instruction|SI\s+executed|nach)"""
    )

    // ── Amount ────────────────────────────────────────────────────────────────

    // PRD §5: Rs\.?\s?[\d,]+(\.\d{2})? | INR\s?[\d,]+(\.\d{2})? | ₹
    private val AMOUNT_RE_V2 = Regex("""(?:Rs\.?\s*|INR\s*|₹\s*)([\d,]+(?:\.\d{1,2})?)""")

    // ── Date ──────────────────────────────────────────────────────────────────

    // PRD §5: (\d{1,2}[\-/]\d{1,2}[\-/]\d{2,4}) | (\d{1,2}\-[A-Za-z]{3}\-\d{2,4})
    private val DATE_ALPHA_RE   = Regex("""(\d{1,2}[-\s][A-Za-z]{3}[-\s]\d{2,4})""")
    private val DATE_COMPACT_RE = Regex("""(\d{1,2}[A-Za-z]{3}\d{2,4})""")
    private val DATE_NUMERIC_RE = Regex("""(\d{1,2}[/\-]\d{1,2}[/\-]\d{2,4})""")

    // ── Account last-4 ────────────────────────────────────────────────────────

    // PRD §5: [AXa-z]{0,4}[Cc]?[/\s]?[Xx*]{1,8}(\d{4})
    private val ACCT_PREFIX_RE = Regex(
        """(?:a/c|acct|account|loan\s+(?:a/c|no\.?)|ending|acc(?:ount)?\s*no\.?)[\s:]*(?:[Xx*]+)?(\d{3,4})""",
        RegexOption.IGNORE_CASE,
    )
    private val ACCT_MASKED_RE = Regex("""[Xx*]{2,8}(\d{4})""")

    // ── UPI VPA ───────────────────────────────────────────────────────────────

    // PRD §5: [\w.\-]+@[\w]+
    private val VPA_CANDIDATE_RE = Regex("""[\w.\-]+@[A-Za-z\d]+""")

    // ── Transaction action keywords ───────────────────────────────────────────

    // PRD §5.2 debit-side
    private val DEBIT_RE = Regex("""(?i)(?:debited?|paid|withdrawn?|deducted?|processed?|auto.?debit)""")
    // PRD §5.2 credit-side
    private val CREDIT_RE = Regex("""(?i)(?:credited?|received|deposited?)""")

    // ── Category signals ──────────────────────────────────────────────────────

    // PRD §5: (?i)(\bEMI\b|equated monthly|loan instalment|loan A\/c|loan account)
    private val EMI_RE = Regex(
        """(?i)(?:\bEMI\b|equated\s+monthly|loan\s+instalment|loan\s+installment|loan\s+a/c|loan\s+account)"""
    )

    // PRD §5: (?i)(\bsalary\b|\bsal\b credited|\bpayroll\b|\bwages\b)
    private val SALARY_RE = Regex("""(?i)(?:\bsalary\b|\bsal\b|\bpayroll\b|\bwages\b)""")

    private val NEFT_IMPS_RE   = Regex("""(?i)(?:\bNEFT\b|\bIMPS\b|\bRTGS\b)""")
    private val ATM_RE         = Regex("""(?i)(?:\bATM\b|\bcash\s+withdrawal\b)""")
    private val UPI_SIGNAL_RE  = Regex("""(?i)\bUPI\b""")

    private val CC_BILL_RE = Regex(
        """(?i)(?:credit\s+card\s+(?:bill|payment|due|statement)|(?:min(?:imum)?|total)\s+(?:amount\s+)?due|card\s+(?:bill|dues?))"""
    )

    private val UTILITY_RE = Regex(
        """(?i)(?:\belectricity\b|\bgas\s+bill\b|\bwater\s+bill\b|\bbroadband\b|\bBBPS\b|\butility\s+bill\b)"""
    )

    private val INVESTMENT_RE = Regex(
        """(?i)(?:\bSIP\b|\bmutual\s+fund\b|\bNPS\b|\bPPF\b\s+(?:credited|deposited)|\bMF\s+(?:purchase|redemption)\b)"""
    )

    private val INSURANCE_RE = Regex("""(?i)(?:\bpremium\b)""")

    private val LOAN_DISBURSAL_RE = Regex(
        """(?i)(?:disbursed?|sanctioned?|loan\s+(?:amount\s+)?(?:credited|released|transferred)|loan\s+disburs)"""
    )

    private val TELECOM_RECHARGE_RE = Regex(
        """(?i)(?:recharge|prepaid\s+(?:plan|pack)|data\s+(?:pack|plan|add.?on))"""
    )

    private val SUBSCRIPTION_RE = Regex(
        """(?i)(?:\bsubscription\b|\brenew(?:al|ed)\b|\bNetflix\b|\bSpotify\b|\bAmazon\s*Prime\b|\bDisney\+?\s*Hotstar\b|\bZEE5\b|\bSonyLIV\b|\bYouTube\s*Premium\b)"""
    )

    private val REFUND_RE = Regex("""(?i)(?:\brefund(?:ed)?\b|\breversal\b)""")

    private val BALANCE_ALERT_RE = Regex(
        """(?i)(?:avl\s*bal|available\s*bal(?:ance)?|a/c\s*balance|account\s*balance|bal\s*(?:is|:|=)\s*(?:Rs|INR|₹))"""
    )

    private val CATEGORY_KEYWORD_RE = Regex(
        """(?i)(?:\bEMI\b|equated\s+monthly|\bsalary\b|\bpremium\b|\brecharge\b|\bSIP\b|\bsubscription\b)"""
    )

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Parses [body] from [senderAddress] into a [ParsedTransaction].
     *
     * Returns null for:
     *  - OTP messages (AC #2)
     *  - Pure promotional SMS with no financial content (AC #2)
     *  - Unknown sender ID with no known bank mention in body (AC #4)
     *
     * Returns a [ParsedTransaction] with [ParsedTransaction.confidenceScore] ≤ 50 for
     * known-sender messages that lack amount/date; callers should skip import but may
     * log the score for analytics (AC #3, PRD §5.4).
     */
    fun parseTransaction(senderAddress: String, body: String): ParsedTransaction? {
        // 1. OTP — never create a record
        if (OTP_RE.containsMatchIn(body)) return null

        // 2. Promotional with no financial content — ignore
        if (PROMO_RE.containsMatchIn(body) && !FINANCIAL_KEYWORD_RE.containsMatchIn(body)) return null

        // 3. Sender gate — unknown senders pass only if body has any financial keyword + amount (NBFC fallback)
        val knownById   = isKnownSenderId(senderAddress)
        val knownByBody = BODY_BANK_RE.containsMatchIn(body)
        val senderPoints: Int
        if (!knownById && !knownByBody) {
            val hasFinancialSignal = FINANCIAL_KEYWORD_RE.containsMatchIn(body) && AMOUNT_RE_V2.containsMatchIn(body)
            if (!hasFinancialSignal) return null
            senderPoints = 10
        } else {
            senderPoints = if (knownById) 35 else 20
        }

        // 4. Balance-alert gate — pure balance notifications carry no transaction action verb
        if (!DEBIT_RE.containsMatchIn(body) && !CREDIT_RE.containsMatchIn(body)
            && BALANCE_ALERT_RE.containsMatchIn(body)) return null

        // 5. Extract fields
        val amount       = extractAmountV2(body)
        val date         = extractDateV2(body)
        val accountLast4 = extractAccountLast4(body)
        val vpa          = extractVpa(body)
        val merchantName = vpa?.substringBefore('@')?.takeIf { it.isNotBlank() }

        // 6. Classify (PRD §5.2 / HEL-586 §3.2)
        val category = classify(body, vpa)

        // 7. Confidence score (HEL-586 §5.4)
        val confidence = computeConfidence(senderPoints, amount, date, body)

        return ParsedTransaction(
            category        = category,
            amount          = amount ?: 0.0,
            date            = date,
            accountLast4    = accountLast4,
            vpa             = vpa,
            merchantName    = merchantName,
            senderAddress   = senderAddress,
            confidenceScore = confidence,
            rawBody         = body,
        )
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private fun isKnownSenderId(address: String): Boolean {
        val upper = address.uppercase().trim()
        // DLT format: "AX-HDFCBK" → strip 2-char telecom prefix
        val stripped = if (upper.length > 3 && upper[2] == '-') upper.substring(3) else upper
        return stripped in KNOWN_SENDER_IDS || SENDER_BANK_KEYWORD_RE.containsMatchIn(upper)
    }

    private fun extractAmountV2(body: String): Double? =
        AMOUNT_RE_V2.find(body)?.groupValues?.get(1)?.replace(",", "")?.toDoubleOrNull()

    private fun extractDateV2(body: String): String? =
        DATE_ALPHA_RE.find(body)?.value
            ?: DATE_COMPACT_RE.find(body)?.value
            ?: DATE_NUMERIC_RE.find(body)?.value

    private fun extractAccountLast4(body: String): String? {
        ACCT_PREFIX_RE.find(body)?.groupValues?.getOrNull(1)?.takeIf { it.isNotEmpty() }
            ?.let { return it }
        return ACCT_MASKED_RE.find(body)?.groupValues?.getOrNull(1)?.takeIf { it.isNotEmpty() }
    }

    private fun extractVpa(body: String): String? =
        VPA_CANDIDATE_RE.findAll(body).firstOrNull { match ->
            // Email addresses (e.g. support@hdfcbank.com) have a TLD: dot followed by letters.
            // Sentence-ending periods (e.g. "merchant@paytm. Ref:") are safe — the char
            // after the dot is a space or digit, not a letter.
            val endIdx = match.range.last + 1
            val nextChar     = body.getOrNull(endIdx)
            val charAfterDot = body.getOrNull(endIdx + 1)
            val isEmailTld   = nextChar == '.' && charAfterDot?.isLetter() == true
            !isEmailTld
        }?.value

    /**
     * HEL-586 §3.2 classification table — 19 categories evaluated in priority order.
     */
    private fun classify(body: String, vpa: String?): TransactionCategory = when {
        // P1 — loan repayment (EMI debit must beat generic debit)
        EMI_RE.containsMatchIn(body) && DEBIT_RE.containsMatchIn(body)          -> TransactionCategory.EMI_DEBIT
        // P2 — salary / payroll credit
        SALARY_RE.containsMatchIn(body) && CREDIT_RE.containsMatchIn(body)      -> TransactionCategory.SALARY_CREDIT
        // P3 — cash withdrawal
        ATM_RE.containsMatchIn(body)                                             -> TransactionCategory.ATM_WITHDRAWAL
        // P4 — credit card bill / dues
        CC_BILL_RE.containsMatchIn(body)                                         -> TransactionCategory.CREDIT_CARD_BILL
        // P5 — insurance premium
        INSURANCE_RE.containsMatchIn(body)                                       -> TransactionCategory.INSURANCE_PREMIUM
        // P6 — utility bill
        UTILITY_RE.containsMatchIn(body)                                         -> TransactionCategory.UTILITY_BILL
        // P7 — loan disbursal (credit with disbursal keyword)
        LOAN_DISBURSAL_RE.containsMatchIn(body)                                  -> TransactionCategory.LOAN_DISBURSAL
        // P8 — refund / reversal
        REFUND_RE.containsMatchIn(body)                                          -> TransactionCategory.REFUND
        // P9 — investments (SIP / MF)
        INVESTMENT_RE.containsMatchIn(body)                                      -> TransactionCategory.INVESTMENT
        // P10 — UPI credit with VPA
        vpa != null && CREDIT_RE.containsMatchIn(body)                           -> TransactionCategory.UPI_CREDIT
        // P11 — telecom recharge / prepaid
        TELECOM_RECHARGE_RE.containsMatchIn(body)                                -> TransactionCategory.TELECOM_RECHARGE
        // P12 — subscription / renewal
        SUBSCRIPTION_RE.containsMatchIn(body)                                    -> TransactionCategory.SUBSCRIPTION
        // P13 — UPI debit with VPA
        vpa != null                                                               -> TransactionCategory.UPI_DEBIT
        // P14 — UPI credit (signal only, no VPA)
        UPI_SIGNAL_RE.containsMatchIn(body) && CREDIT_RE.containsMatchIn(body)  -> TransactionCategory.UPI_CREDIT
        // P15 — bank transfer credit (NEFT/IMPS/RTGS in)
        NEFT_IMPS_RE.containsMatchIn(body) && CREDIT_RE.containsMatchIn(body)   -> TransactionCategory.BANK_TRANSFER_CREDIT
        // P16 — bank transfer debit (NEFT/IMPS/RTGS out)
        NEFT_IMPS_RE.containsMatchIn(body)                                       -> TransactionCategory.BANK_TRANSFER_DEBIT
        // P17 — UPI debit (signal only, no VPA)
        UPI_SIGNAL_RE.containsMatchIn(body)                                      -> TransactionCategory.UPI_DEBIT
        else                                                                      -> TransactionCategory.UNKNOWN
    }

    // ── Public helpers for BankAccount resolution ─────────────────────────────

    /** Strips the 2-char telecom prefix from DLT sender IDs (e.g. "AX-HDFCBK" → "HDFCBK"). */
    fun normalizeSenderId(address: String): String {
        val upper = address.uppercase().trim()
        return if (upper.length > 3 && upper[2] == '-') upper.substring(3) else upper
    }

    /** Returns a human-readable bank name from the sender ID or body text. */
    fun extractBankName(senderAddress: String, body: String): String {
        BODY_BANK_RE.find(body)?.value?.trim()?.let { return it }
        val upper = senderAddress.uppercase()
        return when {
            "HDFC" in upper    -> "HDFC Bank"
            "SBI" in upper     -> "State Bank of India"
            "ICICI" in upper   -> "ICICI Bank"
            "AXIS" in upper || "UTIBOP" in upper -> "Axis Bank"
            "KOTAK" in upper   -> "Kotak Mahindra Bank"
            "PNB" in upper     -> "Punjab National Bank"
            "BOIIND" in upper  -> "Bank of India"
            "CANARA" in upper  -> "Canara Bank"
            "UCO" in upper     -> "UCO Bank"
            "IDBI" in upper    -> "IDBI Bank"
            "YES" in upper     -> "Yes Bank"
            "INDUSLND" in upper || "INDUS" in upper -> "IndusInd Bank"
            "BARODA" in upper  -> "Bank of Baroda"
            "IDFCFB" in upper  -> "IDFC First Bank"
            "FEDBK" in upper || "FEDBNK" in upper -> "Federal Bank"
            "RBLBNK" in upper  -> "RBL Bank"
            "AUBANK" in upper  -> "AU Small Finance Bank"
            "PAYTMB" in upper  -> "Paytm Payments Bank"
            "CENTBNK" in upper -> "Central Bank of India"
            "INDBNK" in upper  -> "Indian Bank"
            else -> senderAddress.substringAfter("-").ifBlank { senderAddress }
        }
    }

    /**
     * HEL-586 §5.4 revised scoring — max 100.
     *   senderPoints: 35 (whitelist) / 20 (body institution match) / 10 (NBFC fallback)
     *   + amount:             30
     *   + date:               20  (was 25)
     *   + action verb:        10  (debit/credit/paid)
     *   + category keyword:    5  (EMI/salary/premium/recharge/SIP/subscription)
     */
    private fun computeConfidence(
        senderPoints: Int,
        amount: Double?,
        date: String?,
        body: String,
    ): Int {
        var score = senderPoints
        if (amount != null) score += 30
        if (date != null) score += 20
        if (DEBIT_RE.containsMatchIn(body) || CREDIT_RE.containsMatchIn(body)) score += 10
        if (CATEGORY_KEYWORD_RE.containsMatchIn(body)) score += 5
        return minOf(score, 100)
    }
}
