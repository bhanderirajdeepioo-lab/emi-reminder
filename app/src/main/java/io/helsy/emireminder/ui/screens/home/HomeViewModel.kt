package io.helsy.emireminder.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.helsy.emireminder.data.db.entity.Loan
import io.helsy.emireminder.data.repository.LoanRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    loanRepository: LoanRepository,
) : ViewModel() {

    val activeLoans = loanRepository.getActiveLoans()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList<Loan>())
}
