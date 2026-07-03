package com.emireminder.app.ui.screens.smsdashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emireminder.app.data.db.dao.ParsedTransactionDao
import com.emireminder.app.data.db.entity.ParsedTransaction
import com.emireminder.app.data.preferences.UserPreferencesRepository
import com.emireminder.app.domain.model.TransactionCategory
import com.emireminder.app.domain.model.TransactionDirection
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class SmsDashboardViewModel @Inject constructor(
    private val transactionDao: ParsedTransactionDao,
    private val prefsRepository: UserPreferencesRepository,
) : ViewModel() {

    private val fmt = DateTimeFormatter.ofPattern("yyyy-MM")
    private val _yearMonth = MutableStateFlow(YearMonth.now().format(fmt))
    val selectedYearMonth: StateFlow<String> = _yearMonth.asStateFlow()

    private val currentTxns: Flow<List<ParsedTransaction>> = _yearMonth
        .flatMapLatest { transactionDao.getByMonth(it) }

    private val previousTxns: Flow<List<ParsedTransaction>> = _yearMonth
        .flatMapLatest { ym ->
            val prev = YearMonth.parse(ym, fmt).minusMonths(1).format(fmt)
            transactionDao.getByMonth(prev)
        }

    val uiState: StateFlow<SmsDashboardUiState> = combine(
        _yearMonth,
        currentTxns,
        previousTxns,
        prefsRepository.userPreferences.map { it.currencySymbol },
    ) { ym, current, previous, currency ->
        SmsDashboardUiState(
            selectedYearMonth = ym,
            isLoading = false,
            summary = computeSummary(current),
            previousSummary = if (previous.isNotEmpty()) computeSummary(previous) else null,
            categorySummaries = buildCategories(current),
            currencySymbol = currency,
            hasTransactions = current.isNotEmpty(),
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SmsDashboardUiState(selectedYearMonth = _yearMonth.value),
    )

    fun previousMonth() {
        _yearMonth.value = YearMonth.parse(_yearMonth.value, fmt).minusMonths(1).format(fmt)
    }

    fun nextMonth() {
        val next = YearMonth.parse(_yearMonth.value, fmt).plusMonths(1)
        if (!next.isAfter(YearMonth.now())) {
            _yearMonth.value = next.format(fmt)
        }
    }

    private fun computeSummary(txns: List<ParsedTransaction>): MonthlySummaryData {
        val income = txns
            .filter { it.direction == TransactionDirection.CREDIT && it.category == TransactionCategory.INCOME }
            .sumOf { it.amount }
        val emi = txns
            .filter { it.direction == TransactionDirection.DEBIT && it.category == TransactionCategory.EMI_AND_LOANS }
            .sumOf { it.amount }
        val expenses = txns
            .filter { it.direction == TransactionDirection.DEBIT && it.category != TransactionCategory.EMI_AND_LOANS }
            .sumOf { it.amount }
        return MonthlySummaryData(income, emi, expenses, income - emi - expenses)
    }

    private fun buildCategories(txns: List<ParsedTransaction>): List<CategorySummary> =
        txns.groupBy { it.category }
            .map { (cat, list) ->
                CategorySummary(
                    category = cat,
                    totalAmount = list.sumOf { it.amount },
                    transactionCount = list.size,
                    transactions = list.sortedByDescending { it.transactionDate },
                )
            }
            .sortedByDescending { it.totalAmount }
}
