package com.emireminder.app.ui.screens.smsdashboard

import com.emireminder.app.data.db.entity.BankAccount
import com.emireminder.app.data.db.entity.ParsedTransaction
import com.emireminder.app.domain.model.TransactionCategory
import com.emireminder.app.domain.model.TransactionDirection

data class MonthlySummaryData(
    val totalIncome: Double = 0.0,
    val totalEmi: Double = 0.0,
    val totalExpenses: Double = 0.0,
    val netSavings: Double = 0.0,
)

data class CategorySummary(
    val category: TransactionCategory,
    val totalAmount: Double,
    val transactionCount: Int,
    val transactions: List<ParsedTransaction>,
)

/** Per-account spending summary for the selected month. */
data class AccountSummary(
    val account: BankAccount,
    val totalAmount: Double,
    val transactionCount: Int,
) {
    /** Display name: user label if confirmed, else "BankName ·· last4". */
    val displayName: String get() = when {
        account.accountLabel != null -> account.accountLabel
        else -> "${account.bankName} ·· ${account.accountLast4}"
    }
}

data class SmsDashboardUiState(
    val selectedYearMonth: String = "",
    val isLoading: Boolean = true,
    val summary: MonthlySummaryData = MonthlySummaryData(),
    val previousSummary: MonthlySummaryData? = null,
    val categorySummaries: List<CategorySummary> = emptyList(),
    /** Accounts that have a sibling account at the same bank and haven't been labelled yet. */
    val accountsNeedingLabel: List<BankAccount> = emptyList(),
    val accountSummaries: List<AccountSummary> = emptyList(),
    val currencySymbol: String = "₹",
    val hasTransactions: Boolean = false,
    val smsHistoricalScanDone: Boolean = false,
    val error: String? = null,
    /** Non-null when the edit bottom sheet is open. */
    val editingTransaction: ParsedTransaction? = null,
    val editSaveInProgress: Boolean = false,
    val editSaveError: String? = null,
)
