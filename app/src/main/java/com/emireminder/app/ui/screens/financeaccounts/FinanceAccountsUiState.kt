package com.emireminder.app.ui.screens.financeaccounts

import com.emireminder.app.data.db.entity.BankAccount

sealed interface FinanceAccountsUiState {
    data object Loading : FinanceAccountsUiState
    data class Success(val accounts: List<BankAccount>) : FinanceAccountsUiState
    data object Empty : FinanceAccountsUiState
}
