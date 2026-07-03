package com.emireminder.app.ui.screens.financeaccounts

import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import com.emireminder.app.data.db.entity.BankAccount
import com.emireminder.app.data.repository.BankAccountRepository

@OptIn(ExperimentalCoroutinesApi::class)
class FinanceAccountsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val bankAccountRepository: BankAccountRepository = mockk(relaxed = true)
    private lateinit var viewModel: FinanceAccountsViewModel

    private fun makeAccount(
        id: String = "acc-1",
        senderId: String = "HDFCBK",
        bankName: String = "HDFC Bank",
        last4: String = "1234",
        label: String? = null,
    ) = BankAccount(
        id = id,
        senderId = senderId,
        bankName = bankName,
        accountLast4 = last4,
        accountLabel = label,
        firstSeenAt = System.currentTimeMillis(),
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        every { bankAccountRepository.getAllAccounts() } returns flowOf(emptyList())
        viewModel = FinanceAccountsViewModel(bankAccountRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `empty list produces Empty state`() = runTest {
        every { bankAccountRepository.getAllAccounts() } returns flowOf(emptyList())
        val vm = FinanceAccountsViewModel(bankAccountRepository)
        advanceUntilIdle()
        assertTrue(vm.uiState.value is FinanceAccountsUiState.Empty)
    }

    @Test
    fun `non-empty list produces Success state with all accounts`() = runTest {
        val accounts = listOf(makeAccount("a1"), makeAccount("a2", last4 = "5678"))
        every { bankAccountRepository.getAllAccounts() } returns flowOf(accounts)
        val vm = FinanceAccountsViewModel(bankAccountRepository)
        advanceUntilIdle()
        val state = vm.uiState.value
        assertTrue(state is FinanceAccountsUiState.Success)
        assertEquals(2, (state as FinanceAccountsUiState.Success).accounts.size)
    }

    @Test
    fun `renameAccount delegates to repository`() = runTest {
        every { bankAccountRepository.getAllAccounts() } returns flowOf(listOf(makeAccount()))
        val vm = FinanceAccountsViewModel(bankAccountRepository)
        vm.renameAccount("acc-1", "Salary Account")
        advanceUntilIdle()
        coVerify { bankAccountRepository.renameAccount("acc-1", "Salary Account") }
    }

    @Test
    fun `accounts sorted by bank name in Success state`() = runTest {
        val accounts = listOf(
            makeAccount("b1", bankName = "Yes Bank", last4 = "1111"),
            makeAccount("b2", bankName = "HDFC Bank", last4 = "2222"),
        )
        every { bankAccountRepository.getAllAccounts() } returns flowOf(accounts)
        val vm = FinanceAccountsViewModel(bankAccountRepository)
        advanceUntilIdle()
        val state = vm.uiState.value as FinanceAccountsUiState.Success
        // BankAccountDao.getAll() orders by bank_name ASC — first item is HDFC Bank
        assertEquals("HDFC Bank", state.accounts.first().bankName)
    }
}
