package com.emireminder.app.data.repository

import com.emireminder.app.data.db.dao.MonthlyFinanceSummaryDao
import com.emireminder.app.data.db.dao.ParsedTransactionDao
import com.emireminder.app.data.db.entity.MonthlyFinanceSummary
import com.emireminder.app.data.db.entity.ParsedTransaction as ParsedTransactionEntity
import com.emireminder.app.domain.model.TransactionCategory
import com.emireminder.app.domain.model.TransactionDirection
import com.emireminder.app.sms.MerchantClassifier
import com.emireminder.app.sms.SmsParser
import com.emireminder.app.sms.TransactionCategory as SmsCategory
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Handles real-time financial SMS processing for the SMS Finance Intelligence feature (HEL-568).
 *
 * Call [processIncomingSms] from the BroadcastReceiver for each incoming message.
 * Non-financial messages (OTPs, promos, unknown senders) are silently ignored.
 */
@Singleton
class SmsFinanceRepository @Inject constructor(
    private val parsedTransactionDao: ParsedTransactionDao,
    private val bankAccountRepository: BankAccountRepository,
    private val monthlyFinanceSummaryDao: MonthlyFinanceSummaryDao,
    private val autoDetectedEmiRepository: AutoDetectedEmiRepository,
) {

    /**
     * Processes one incoming real-time SMS.
     *
     * @param senderAddress the originating address (e.g. "VK-HDFCBK")
     * @param body the raw SMS body
     * @param receivedAtMs timestamp of receipt in epoch milliseconds (from SMS PDU)
     * @return true if the message was a financial transaction that was stored, false if ignored
     */
    suspend fun processIncomingSms(
        senderAddress: String,
        body: String,
        receivedAtMs: Long = System.currentTimeMillis(),
    ): Boolean {
        val parsed = SmsParser.parseTransaction(senderAddress, body) ?: return false
        if (!parsed.isImportEligible) return false

        val last4 = parsed.accountLast4 ?: "XXXX"
        val bankName = SmsParser.extractBankName(senderAddress, body)
        val bankAccountId = bankAccountRepository.resolveAccount(senderAddress, bankName, last4)

        val enrichedCategory = enrichWithMerchant(parsed.category, body, parsed.vpa, parsed.merchantName)
        val direction = mapDirection(enrichedCategory)
        val domainCategory = mapCategory(enrichedCategory)
        val transactionDate = parseTransactionDate(parsed.date, receivedAtMs)
        val yearMonth = formatYearMonth(transactionDate)
        val isEmi = parsed.category == SmsCategory.EMI_DEBIT
        val smsId = "${senderAddress}:${receivedAtMs}"

        val entity = ParsedTransactionEntity(
            id               = UUID.randomUUID().toString(),
            smsId            = smsId,
            parsedAt         = System.currentTimeMillis(),
            transactionDate  = transactionDate,
            yearMonth        = yearMonth,
            amount           = parsed.amount,
            direction        = direction,
            category         = domainCategory,
            senderId         = senderAddress,
            bankName         = bankName,
            accountLast4     = last4,
            bankAccountId    = bankAccountId,
            merchantName     = parsed.merchantName,
            vpa              = parsed.vpa,
            confidenceScore  = parsed.confidenceScore,
            isEmi            = isEmi,
            loanAccountLast4 = if (isEmi) last4 else null,
            rawSmsBody       = body,
        )

        val rowId = parsedTransactionDao.insert(entity)
        if (rowId != -1L) {
            updateMonthSummary(yearMonth, entity)
            if (isEmi) {
                autoDetectedEmiRepository.processEmiTransaction(entity)
            }
        }
        return rowId != -1L
    }

    /**
     * Processes one historical SMS read from the ContentProvider during the first-time scan.
     * Uses the ContentProvider's `_id` as [smsId] so duplicates are detected on re-scan.
     *
     * @return true if inserted, false if skipped (low confidence, ignored type, duplicate)
     */
    suspend fun processHistoricalSms(
        smsId: String,
        senderAddress: String,
        body: String,
        dateMs: Long,
    ): Boolean {
        val parsed = SmsParser.parseTransaction(senderAddress, body) ?: return false
        if (!parsed.isImportEligible) return false

        val last4 = parsed.accountLast4 ?: "XXXX"
        val bankName = SmsParser.extractBankName(senderAddress, body)
        val bankAccountId = bankAccountRepository.resolveAccount(senderAddress, bankName, last4)

        val enrichedCategory = enrichWithMerchant(parsed.category, body, parsed.vpa, parsed.merchantName)
        val direction = mapDirection(enrichedCategory)
        val domainCategory = mapCategory(enrichedCategory)
        val transactionDate = parseTransactionDate(parsed.date, dateMs)
        val yearMonth = formatYearMonth(transactionDate)
        val isEmi = parsed.category == SmsCategory.EMI_DEBIT

        val entity = ParsedTransactionEntity(
            id               = UUID.randomUUID().toString(),
            smsId            = smsId,
            parsedAt         = System.currentTimeMillis(),
            transactionDate  = transactionDate,
            yearMonth        = yearMonth,
            amount           = parsed.amount,
            direction        = direction,
            category         = domainCategory,
            senderId         = senderAddress,
            bankName         = bankName,
            accountLast4     = last4,
            bankAccountId    = bankAccountId,
            merchantName     = parsed.merchantName,
            vpa              = parsed.vpa,
            confidenceScore  = parsed.confidenceScore,
            isEmi            = isEmi,
            loanAccountLast4 = if (isEmi) last4 else null,
            rawSmsBody       = body,
        )

        val rowId = parsedTransactionDao.insert(entity)
        if (rowId != -1L) {
            updateMonthSummary(yearMonth, entity)
            if (isEmi) {
                autoDetectedEmiRepository.processEmiTransaction(entity)
            }
        }
        return rowId != -1L
    }

    /**
     * Permanently erases all SMS-derived finance data (DPDP Act 2023 Right to Erasure).
     * Clears transactions, monthly summaries, auto-detected EMIs, and bank account metadata.
     */
    suspend fun deleteAllFinanceData() {
        parsedTransactionDao.deleteAll()
        monthlyFinanceSummaryDao.deleteAll()
        autoDetectedEmiRepository.deleteAll()
        bankAccountRepository.deleteAll()
    }

    // ── Private helpers ────────────────────────────────────────────────────────

    private suspend fun updateMonthSummary(
        yearMonth: String,
        tx: ParsedTransactionEntity,
    ) {
        val existing = monthlyFinanceSummaryDao.getByYearMonth(yearMonth)
        val isCredit = tx.direction == TransactionDirection.CREDIT
        val amt = tx.amount

        val totalIncome   = (existing?.totalIncome ?: 0.0) + if (isCredit) amt else 0.0
        val totalEmi      = (existing?.totalEmi ?: 0.0) + if (tx.isEmi) amt else 0.0
        val totalExpenses = (existing?.totalExpenses ?: 0.0) +
            if (!isCredit && !tx.isEmi) amt else 0.0

        val topCategories = existing?.topCategories?.toMutableMap() ?: mutableMapOf()
        val catKey = tx.category.name
        topCategories[catKey] = (topCategories[catKey] ?: 0.0) + amt

        val summary = MonthlyFinanceSummary(
            yearMonth     = yearMonth,
            totalIncome   = totalIncome,
            totalEmi      = totalEmi,
            totalExpenses = totalExpenses,
            topCategories = topCategories,
            netSavings    = totalIncome - totalEmi - totalExpenses,
            createdAt     = existing?.createdAt ?: System.currentTimeMillis(),
        )
        monthlyFinanceSummaryDao.insert(summary)
    }

    private fun enrichWithMerchant(
        category: SmsCategory,
        body: String,
        vpa: String?,
        merchantName: String?,
    ): SmsCategory {
        if (category != SmsCategory.UPI_DEBIT && category != SmsCategory.UNKNOWN) return category
        return MerchantClassifier.classify(body, vpa, merchantName) ?: category
    }

    private fun mapDirection(category: SmsCategory): TransactionDirection = when (category) {
        SmsCategory.SALARY_CREDIT,
        SmsCategory.UPI_CREDIT,
        SmsCategory.BANK_TRANSFER_CREDIT,
        SmsCategory.LOAN_DISBURSAL,
        SmsCategory.REFUND              -> TransactionDirection.CREDIT
        else                            -> TransactionDirection.DEBIT
    }

    private fun mapCategory(category: SmsCategory): TransactionCategory = when (category) {
        SmsCategory.EMI_DEBIT              -> TransactionCategory.EMI_AND_LOANS
        SmsCategory.SALARY_CREDIT          -> TransactionCategory.INCOME
        SmsCategory.UPI_CREDIT             -> TransactionCategory.INCOME
        SmsCategory.UPI_DEBIT              -> TransactionCategory.UNCATEGORISED
        SmsCategory.BANK_TRANSFER_DEBIT,
        SmsCategory.BANK_TRANSFER_CREDIT   -> TransactionCategory.UNCATEGORISED
        SmsCategory.ATM_WITHDRAWAL         -> TransactionCategory.ATM_AND_CASH
        SmsCategory.CREDIT_CARD_BILL       -> TransactionCategory.CREDIT_CARD
        SmsCategory.UTILITY_BILL           -> TransactionCategory.UTILITIES
        SmsCategory.INVESTMENT             -> TransactionCategory.INVESTMENTS
        SmsCategory.INSURANCE_PREMIUM      -> TransactionCategory.INSURANCE
        SmsCategory.LOAN_DISBURSAL         -> TransactionCategory.INCOME
        SmsCategory.TELECOM_RECHARGE       -> TransactionCategory.UTILITIES
        SmsCategory.SUBSCRIPTION           -> TransactionCategory.ENTERTAINMENT
        SmsCategory.REFUND                 -> TransactionCategory.INCOME
        SmsCategory.FOOD_AND_DINING        -> TransactionCategory.FOOD_AND_DINING
        SmsCategory.TRANSPORT              -> TransactionCategory.TRANSPORT
        SmsCategory.SHOPPING               -> TransactionCategory.SHOPPING
        SmsCategory.HEALTH                 -> TransactionCategory.HEALTH
        SmsCategory.BANK_CHARGES           -> TransactionCategory.BANK_CHARGES
        SmsCategory.BALANCE_ALERT,
        SmsCategory.IGNORED,
        SmsCategory.UNKNOWN                -> TransactionCategory.UNCATEGORISED
    }

    private fun parseTransactionDate(dateStr: String?, fallback: Long): Long {
        if (dateStr == null) return fallback
        val formats = listOf(
            "dd-MMM-yy", "dd MMM yy", "ddMMMyy",
            "dd-MMM-yyyy", "dd MMM yyyy", "ddMMMyyyy",
            "dd/MM/yy", "dd/MM/yyyy", "dd-MM-yy", "dd-MM-yyyy",
        )
        for (fmt in formats) {
            try {
                val sdf = SimpleDateFormat(fmt, Locale.ENGLISH)
                sdf.isLenient = false
                sdf.parse(dateStr)?.time?.let { return it }
            } catch (_: Exception) { }
        }
        return fallback
    }

    private fun formatYearMonth(epochMs: Long): String {
        val local = Instant.ofEpochMilli(epochMs)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
        return DateTimeFormatter.ofPattern("yyyy-MM").format(local)
    }

    // ── Transaction CRUD (called by SmsDashboardViewModel) ─────────────────────

    suspend fun deleteTransaction(transaction: ParsedTransactionEntity) {
        parsedTransactionDao.delete(transaction)
    }

    suspend fun insertTransaction(transaction: ParsedTransactionEntity): Long =
        parsedTransactionDao.insert(transaction)

    suspend fun updateTransaction(transaction: ParsedTransactionEntity) {
        parsedTransactionDao.update(transaction)
    }

}
