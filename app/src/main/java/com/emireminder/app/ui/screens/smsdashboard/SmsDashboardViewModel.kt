package com.emireminder.app.ui.screens.smsdashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emireminder.app.data.db.dao.AutoDetectedEmiDao
import com.emireminder.app.data.db.dao.ParsedTransactionDao
import com.emireminder.app.data.db.entity.BankAccount
import com.emireminder.app.data.db.entity.ParsedTransaction
import com.emireminder.app.data.preferences.UserPreferencesRepository
import com.emireminder.app.data.repository.BankAccountRepository
import com.emireminder.app.domain.model.TransactionCategory
import com.emireminder.app.domain.model.TransactionDirection
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class SmsDashboardViewModel @Inject constructor(
    private val transactionDao: ParsedTransactionDao,
    private val autoDetectedEmiDao: AutoDetectedEmiDao,
    private val bankAccountRepository: BankAccountRepository,
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

    private val allAccounts: Flow<List<BankAccount>> = bankAccountRepository.getAllAccounts()
    private val pendingLabelAccounts: Flow<List<BankAccount>> = bankAccountRepository.getAccountsNeedingPrompt()

    private val _editingTransaction = MutableStateFlow<ParsedTransaction?>(null)
    private val _editSaveInProgress = MutableStateFlow(false)
    private val _editSaveError = MutableStateFlow<String?>(null)
    private val _selectedCategoryFilter = MutableStateFlow<TransactionCategory?>(null)

    private val _baseState: Flow<SmsDashboardUiState> = combine(
        _yearMonth,
        currentTxns,
        previousTxns,
        allAccounts,
        pendingLabelAccounts,
    ) { args ->
        val ym = args[0] as String
        @Suppress("UNCHECKED_CAST")
        val current = args[1] as List<ParsedTransaction>
        @Suppress("UNCHECKED_CAST")
        val previous = args[2] as List<ParsedTransaction>
        @Suppress("UNCHECKED_CAST")
        val accounts = args[3] as List<BankAccount>
        @Suppress("UNCHECKED_CAST")
        val pending = args[4] as List<BankAccount>
        Triple(ym, current to previous, accounts to pending)
    }.combine(prefsRepository.userPreferences) { (ym, txPair, acctPair), prefs ->
        val (current, previous) = txPair
        val (accounts, pending) = acctPair
        SmsDashboardUiState(
            selectedYearMonth = ym,
            isLoading = false,
            summary = computeSummary(current),
            previousSummary = if (previous.isNotEmpty()) computeSummary(previous) else null,
            categorySummaries = buildCategories(current),
            allTransactions = current.sortedByDescending { it.transactionDate },
            categoriesWithTransactions = current.map { it.category }.toSet(),
            accountsNeedingLabel = pending,
            accountSummaries = buildAccountSummaries(current, accounts),
            currencySymbol = prefs.currencySymbol,
            hasTransactions = current.isNotEmpty(),
            smsHistoricalScanDone = prefs.smsHistoricalScanDone,
        )
    }

    val uiState: StateFlow<SmsDashboardUiState> = combine(
        _baseState,
        _editingTransaction,
        _editSaveInProgress,
        _editSaveError,
        _selectedCategoryFilter,
    ) { base, editing, saving, error, filter ->
        base.copy(
            editingTransaction = editing,
            editSaveInProgress = saving,
            editSaveError = error,
            selectedCategoryFilter = filter,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SmsDashboardUiState(selectedYearMonth = _yearMonth.value),
    )

    fun previousMonth() {
        _yearMonth.value = YearMonth.parse(_yearMonth.value, fmt).minusMonths(1).format(fmt)
        _selectedCategoryFilter.value = null
    }

    fun nextMonth() {
        val next = YearMonth.parse(_yearMonth.value, fmt).plusMonths(1)
        if (!next.isAfter(YearMonth.now())) {
            _yearMonth.value = next.format(fmt)
            _selectedCategoryFilter.value = null
        }
    }

    fun setCategoryFilter(category: TransactionCategory?) {
        _selectedCategoryFilter.value = category
    }

    /** Call when the prompt card for [account] is first rendered. Sets label_prompted_at. */
    fun onPromptShown(account: BankAccount) {
        viewModelScope.launch {
            bankAccountRepository.markPromptShown(account.id)
        }
    }

    /** User selected a pre-defined or custom label for [account]. */
    fun applyLabel(account: BankAccount, label: String) {
        viewModelScope.launch {
            bankAccountRepository.applyLabel(account.id, label)
        }
    }

    /** User tapped Skip on the labelling prompt. */
    fun skipLabel(account: BankAccount) {
        viewModelScope.launch {
            bankAccountRepository.skipLabel(account.id)
        }
    }

    // ── Transaction edit sheet ─────────────────────────────────────────────────

    fun openEditSheet(transaction: ParsedTransaction) {
        _editSaveError.value = null
        _editingTransaction.value = transaction
    }

    fun dismissEditSheet() {
        _editingTransaction.value = null
        _editSaveError.value = null
    }

    /**
     * Persists user edits to the selected transaction.
     * Sets [ParsedTransaction.userVerified] = true.
     * If [isEmi] and [lenderName] changed, also updates the linked [AutoDetectedEmi].
     */
    fun saveTransaction(
        id: String,
        amount: Double,
        category: TransactionCategory,
        subCategory: String?,
        merchantName: String?,
        notes: String?,
        lenderName: String?,
    ) {
        val original = _editingTransaction.value ?: return
        _editSaveInProgress.value = true
        _editSaveError.value = null

        viewModelScope.launch {
            try {
                val updated = original.copy(
                    amount = amount,
                    category = category,
                    subCategory = subCategory?.takeIf { it.isNotBlank() },
                    merchantName = merchantName?.takeIf { it.isNotBlank() },
                    notes = notes?.takeIf { it.isNotBlank() },
                    userVerified = true,
                )
                transactionDao.update(updated)

                if (original.isEmi && !lenderName.isNullOrBlank()) {
                    val emi = autoDetectedEmiDao.getByTransactionId(id)
                    if (emi != null && emi.lenderName != lenderName) {
                        autoDetectedEmiDao.update(emi.copy(lenderName = lenderName))
                    }
                }

                _editingTransaction.value = null
            } catch (e: Exception) {
                _editSaveError.value = e.message ?: "Save failed"
            } finally {
                _editSaveInProgress.value = false
            }
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
        return MonthlySummaryData(
            totalIncome = income,
            totalEmi = emi,
            totalExpenses = expenses,
            netSavings = income - emi - expenses,
            totalSpend = emi + expenses,
            transactionCount = txns.size,
        )
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

    private fun buildAccountSummaries(
        txns: List<ParsedTransaction>,
        accounts: List<BankAccount>,
    ): List<AccountSummary> {
        val accountMap = accounts.associateBy { it.id }
        return txns
            .filter { it.direction == TransactionDirection.DEBIT }
            .groupBy { it.bankAccountId }
            .mapNotNull { (accountId, list) ->
                val account = accountMap[accountId] ?: return@mapNotNull null
                AccountSummary(
                    account = account,
                    totalAmount = list.sumOf { it.amount },
                    transactionCount = list.size,
                )
            }
            .sortedByDescending { it.totalAmount }
    }
}
