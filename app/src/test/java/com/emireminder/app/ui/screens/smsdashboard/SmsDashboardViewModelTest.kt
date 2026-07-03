package com.emireminder.app.ui.screens.smsdashboard

import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import com.emireminder.app.data.db.dao.AutoDetectedEmiDao
import com.emireminder.app.data.db.dao.ParsedTransactionDao
import com.emireminder.app.data.db.entity.BankAccount
import com.emireminder.app.data.db.entity.ParsedTransaction
import com.emireminder.app.data.preferences.UserPreferences
import com.emireminder.app.data.preferences.UserPreferencesRepository
import com.emireminder.app.data.repository.BankAccountRepository
import com.emireminder.app.domain.model.TransactionCategory
import com.emireminder.app.domain.model.TransactionDirection
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
class SmsDashboardViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val transactionDao: ParsedTransactionDao = mockk(relaxed = true)
    private val autoDetectedEmiDao: AutoDetectedEmiDao = mockk(relaxed = true)
    private val bankAccountRepository: BankAccountRepository = mockk(relaxed = true)
    private val prefsRepository: UserPreferencesRepository = mockk(relaxed = true)
    private val fmt = DateTimeFormatter.ofPattern("yyyy-MM")

    private lateinit var viewModel: SmsDashboardViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        every { prefsRepository.userPreferences } returns flowOf(UserPreferences(currency = "INR"))
        every { transactionDao.getByMonth(any()) } returns flowOf(emptyList())
        every { bankAccountRepository.getAllAccounts() } returns flowOf(emptyList())
        every { bankAccountRepository.getAccountsNeedingPrompt() } returns flowOf(emptyList())
        viewModel = SmsDashboardViewModel(transactionDao, autoDetectedEmiDao, bankAccountRepository, prefsRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial selectedYearMonth is current month`() {
        assertEquals(YearMonth.now().format(fmt), viewModel.selectedYearMonth.value)
    }

    @Test
    fun `previousMonth navigates back one month`() {
        val current = YearMonth.parse(viewModel.selectedYearMonth.value, fmt)
        viewModel.previousMonth()
        assertEquals(current.minusMonths(1).format(fmt), viewModel.selectedYearMonth.value)
    }

    @Test
    fun `nextMonth does not advance past current month`() {
        val today = YearMonth.now().format(fmt)
        viewModel.nextMonth()
        assertEquals(today, viewModel.selectedYearMonth.value)
    }

    @Test
    fun `nextMonth advances when on a past month`() {
        viewModel.previousMonth()
        viewModel.previousMonth()
        val base = YearMonth.parse(viewModel.selectedYearMonth.value, fmt)
        viewModel.nextMonth()
        assertEquals(base.plusMonths(1).format(fmt), viewModel.selectedYearMonth.value)
    }

    @Test
    fun `uiState hasTransactions is false when no transactions`() = runTest {
        every { transactionDao.getByMonth(any()) } returns flowOf(emptyList())
        viewModel = SmsDashboardViewModel(transactionDao, autoDetectedEmiDao, bankAccountRepository, prefsRepository)
        var state = viewModel.uiState.value
        backgroundScope.launch { viewModel.uiState.collect { state = it } }
        advanceUntilIdle()
        assertFalse(state.hasTransactions)
    }

    @Test
    fun `uiState hasTransactions is true when transactions present`() = runTest {
        val txns = listOf(makeTxn(TransactionCategory.INCOME, TransactionDirection.CREDIT, 50000.0))
        every { transactionDao.getByMonth(any()) } returns flowOf(txns)
        viewModel = SmsDashboardViewModel(transactionDao, autoDetectedEmiDao, bankAccountRepository, prefsRepository)
        var state = viewModel.uiState.value
        backgroundScope.launch { viewModel.uiState.collect { state = it } }
        advanceUntilIdle()
        assertTrue(state.hasTransactions)
    }

    @Test
    fun `summary totalIncome sums only CREDIT INCOME transactions`() = runTest {
        val txns = listOf(
            makeTxn(TransactionCategory.INCOME,          TransactionDirection.CREDIT, 50000.0),
            makeTxn(TransactionCategory.FOOD_AND_DINING, TransactionDirection.DEBIT,   2000.0),
        )
        every { transactionDao.getByMonth(any()) } returns flowOf(txns)
        viewModel = SmsDashboardViewModel(transactionDao, autoDetectedEmiDao, bankAccountRepository, prefsRepository)
        var state = viewModel.uiState.value
        backgroundScope.launch { viewModel.uiState.collect { state = it } }
        advanceUntilIdle()
        assertEquals(50000.0, state.summary.totalIncome, 0.01)
    }

    @Test
    fun `summary totalEmi sums only DEBIT EMI_AND_LOANS transactions`() = runTest {
        val txns = listOf(
            makeTxn(TransactionCategory.EMI_AND_LOANS, TransactionDirection.DEBIT,  15000.0),
            makeTxn(TransactionCategory.INCOME,         TransactionDirection.CREDIT, 50000.0),
        )
        every { transactionDao.getByMonth(any()) } returns flowOf(txns)
        viewModel = SmsDashboardViewModel(transactionDao, autoDetectedEmiDao, bankAccountRepository, prefsRepository)
        var state = viewModel.uiState.value
        backgroundScope.launch { viewModel.uiState.collect { state = it } }
        advanceUntilIdle()
        assertEquals(15000.0, state.summary.totalEmi, 0.01)
    }

    @Test
    fun `summary totalExpenses excludes EMI_AND_LOANS debits`() = runTest {
        val txns = listOf(
            makeTxn(TransactionCategory.EMI_AND_LOANS,   TransactionDirection.DEBIT, 15000.0),
            makeTxn(TransactionCategory.FOOD_AND_DINING, TransactionDirection.DEBIT,  3000.0),
            makeTxn(TransactionCategory.TRANSPORT,        TransactionDirection.DEBIT,  1000.0),
        )
        every { transactionDao.getByMonth(any()) } returns flowOf(txns)
        viewModel = SmsDashboardViewModel(transactionDao, autoDetectedEmiDao, bankAccountRepository, prefsRepository)
        var state = viewModel.uiState.value
        backgroundScope.launch { viewModel.uiState.collect { state = it } }
        advanceUntilIdle()
        assertEquals(4000.0, state.summary.totalExpenses, 0.01)
    }

    @Test
    fun `summary netSavings equals income minus emi minus expenses`() = runTest {
        val txns = listOf(
            makeTxn(TransactionCategory.INCOME,          TransactionDirection.CREDIT, 50000.0),
            makeTxn(TransactionCategory.EMI_AND_LOANS,   TransactionDirection.DEBIT,  10000.0),
            makeTxn(TransactionCategory.FOOD_AND_DINING, TransactionDirection.DEBIT,   5000.0),
        )
        every { transactionDao.getByMonth(any()) } returns flowOf(txns)
        viewModel = SmsDashboardViewModel(transactionDao, autoDetectedEmiDao, bankAccountRepository, prefsRepository)
        var state = viewModel.uiState.value
        backgroundScope.launch { viewModel.uiState.collect { state = it } }
        advanceUntilIdle()
        assertEquals(35000.0, state.summary.netSavings, 0.01)
    }

    @Test
    fun `categorySummaries groups transactions by category`() = runTest {
        val txns = listOf(
            makeTxn(TransactionCategory.FOOD_AND_DINING, TransactionDirection.DEBIT, 1000.0),
            makeTxn(TransactionCategory.FOOD_AND_DINING, TransactionDirection.DEBIT,  500.0),
            makeTxn(TransactionCategory.TRANSPORT,        TransactionDirection.DEBIT,  200.0),
        )
        every { transactionDao.getByMonth(any()) } returns flowOf(txns)
        viewModel = SmsDashboardViewModel(transactionDao, autoDetectedEmiDao, bankAccountRepository, prefsRepository)
        var state = viewModel.uiState.value
        backgroundScope.launch { viewModel.uiState.collect { state = it } }
        advanceUntilIdle()

        val food = state.categorySummaries.find { it.category == TransactionCategory.FOOD_AND_DINING }
        assertNotNull(food)
        assertEquals(2, food!!.transactionCount)
        assertEquals(1500.0, food.totalAmount, 0.01)
    }

    @Test
    fun `categorySummaries sorted descending by total amount`() = runTest {
        val txns = listOf(
            makeTxn(TransactionCategory.TRANSPORT,        TransactionDirection.DEBIT,  200.0),
            makeTxn(TransactionCategory.FOOD_AND_DINING, TransactionDirection.DEBIT, 3000.0),
            makeTxn(TransactionCategory.SHOPPING,         TransactionDirection.DEBIT, 1500.0),
        )
        every { transactionDao.getByMonth(any()) } returns flowOf(txns)
        viewModel = SmsDashboardViewModel(transactionDao, autoDetectedEmiDao, bankAccountRepository, prefsRepository)
        var state = viewModel.uiState.value
        backgroundScope.launch { viewModel.uiState.collect { state = it } }
        advanceUntilIdle()
        val amounts = state.categorySummaries.map { it.totalAmount }
        assertEquals(amounts.sortedDescending(), amounts)
    }

    @Test
    fun `previousSummary is null when previous month has no transactions`() = runTest {
        val currentYm = YearMonth.now().format(fmt)
        val prevYm = YearMonth.now().minusMonths(1).format(fmt)
        every { transactionDao.getByMonth(currentYm) } returns flowOf(
            listOf(makeTxn(TransactionCategory.INCOME, TransactionDirection.CREDIT, 50000.0))
        )
        every { transactionDao.getByMonth(prevYm) } returns flowOf(emptyList())
        viewModel = SmsDashboardViewModel(transactionDao, autoDetectedEmiDao, bankAccountRepository, prefsRepository)
        var state = viewModel.uiState.value
        backgroundScope.launch { viewModel.uiState.collect { state = it } }
        advanceUntilIdle()
        assertNull(state.previousSummary)
    }

    @Test
    fun `previousSummary populated when previous month has transactions`() = runTest {
        val currentYm = YearMonth.now().format(fmt)
        val prevYm = YearMonth.now().minusMonths(1).format(fmt)
        every { transactionDao.getByMonth(currentYm) } returns flowOf(
            listOf(makeTxn(TransactionCategory.INCOME, TransactionDirection.CREDIT, 60000.0))
        )
        every { transactionDao.getByMonth(prevYm) } returns flowOf(
            listOf(makeTxn(TransactionCategory.INCOME, TransactionDirection.CREDIT, 50000.0))
        )
        viewModel = SmsDashboardViewModel(transactionDao, autoDetectedEmiDao, bankAccountRepository, prefsRepository)
        var state = viewModel.uiState.value
        backgroundScope.launch { viewModel.uiState.collect { state = it } }
        advanceUntilIdle()
        assertNotNull(state.previousSummary)
        assertEquals(50000.0, state.previousSummary!!.totalIncome, 0.01)
    }

    private fun makeTxn(
        category: TransactionCategory,
        direction: TransactionDirection,
        amount: Double,
    ) = ParsedTransaction(
        id = UUID.randomUUID().toString(),
        smsId = UUID.randomUUID().toString(),
        parsedAt = System.currentTimeMillis(),
        transactionDate = System.currentTimeMillis(),
        yearMonth = YearMonth.now().format(fmt),
        amount = amount,
        direction = direction,
        category = category,
        senderId = "HDFCBK",
        bankName = "HDFC Bank",
        accountLast4 = "1234",
        bankAccountId = "acc-1",
        confidenceScore = 90,
    )
}
