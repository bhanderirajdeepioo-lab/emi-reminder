package com.emireminder.app.ui.screens.settings

import com.emireminder.app.data.db.entity.ParsedTransaction

sealed interface MyFinanceDataUiState {
    data object Loading : MyFinanceDataUiState
    data class Success(val transactions: List<ParsedTransaction>) : MyFinanceDataUiState
    data object Empty : MyFinanceDataUiState
    data class Error(val message: String) : MyFinanceDataUiState
}
