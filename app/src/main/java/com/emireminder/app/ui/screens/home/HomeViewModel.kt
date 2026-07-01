package com.emireminder.app.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import com.emireminder.app.data.db.entity.Loan
import com.emireminder.app.data.repository.LoanRepository
import com.emireminder.app.data.repository.ReminderRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    loanRepository: LoanRepository,
    reminderRepository: ReminderRepository,
) : ViewModel() {

    val activeLoans = loanRepository.getActiveLoans()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList<Loan>())

    val activeReminderCount = reminderRepository.getActiveReminders()
        .map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)
}
