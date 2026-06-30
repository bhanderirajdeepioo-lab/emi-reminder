package com.emireminder.app.ui.screens.finance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import com.emireminder.app.data.repository.LoanRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class FinanceViewModel @Inject constructor(
    private val loanRepository: LoanRepository,
) : ViewModel() {

    val activeLoans = loanRepository.getActiveLoans()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}
