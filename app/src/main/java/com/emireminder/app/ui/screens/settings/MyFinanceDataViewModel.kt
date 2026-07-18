package com.emireminder.app.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emireminder.app.data.db.dao.ParsedTransactionDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class MyFinanceDataViewModel @Inject constructor(
    private val transactionDao: ParsedTransactionDao,
) : ViewModel() {

    val uiState: StateFlow<MyFinanceDataUiState> = transactionDao.getAll()
        .map { list ->
            if (list.isEmpty()) MyFinanceDataUiState.Empty
            else MyFinanceDataUiState.Success(list)
        }
        .catch { e -> emit(MyFinanceDataUiState.Error(e.message ?: "Failed to load data")) }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            MyFinanceDataUiState.Loading,
        )
}
