package com.emireminder.app.ui.screens.smsdashboard

import com.emireminder.app.data.db.entity.ParsedTransaction
import com.emireminder.app.domain.model.TransactionCategory

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

data class SmsDashboardUiState(
    val selectedYearMonth: String = "",
    val isLoading: Boolean = true,
    val summary: MonthlySummaryData = MonthlySummaryData(),
    val previousSummary: MonthlySummaryData? = null,
    val categorySummaries: List<CategorySummary> = emptyList(),
    val currencySymbol: String = "₹",
    val hasTransactions: Boolean = false,
    val error: String? = null,
)
