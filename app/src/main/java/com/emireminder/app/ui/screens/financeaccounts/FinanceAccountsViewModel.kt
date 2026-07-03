package com.emireminder.app.ui.screens.financeaccounts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emireminder.app.data.repository.BankAccountRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FinanceAccountsViewModel @Inject constructor(
    private val bankAccountRepository: BankAccountRepository,
) : ViewModel() {

    val uiState = bankAccountRepository.getAllAccounts()
        .map { accounts ->
            if (accounts.isEmpty()) FinanceAccountsUiState.Empty
            else FinanceAccountsUiState.Success(accounts.sortedBy { it.bankName })
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = FinanceAccountsUiState.Loading,
        )

    fun renameAccount(accountId: String, label: String) {
        viewModelScope.launch {
            bankAccountRepository.renameAccount(accountId, label)
        }
    }
}
