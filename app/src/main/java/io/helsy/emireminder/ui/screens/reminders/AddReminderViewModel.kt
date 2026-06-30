package io.helsy.emireminder.ui.screens.reminders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.helsy.emireminder.data.db.entity.Loan
import io.helsy.emireminder.data.db.entity.Reminder
import io.helsy.emireminder.data.repository.LoanRepository
import io.helsy.emireminder.data.repository.ReminderRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddReminderViewModel @Inject constructor(
    private val loanRepository: LoanRepository,
    private val reminderRepository: ReminderRepository,
) : ViewModel() {

    val loans = loanRepository.getActiveLoans()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList<Loan>())

    fun saveReminder(
        loanId: Int,
        loanName: String,
        emiAmount: Double,
        dueDayOfMonth: Int,
        notes: String,
        onSuccess: () -> Unit,
    ) = viewModelScope.launch {
        reminderRepository.insertReminder(
            Reminder(
                loanId = loanId,
                loanName = loanName,
                emiAmount = emiAmount,
                dueDayOfMonth = dueDayOfMonth,
                notes = notes,
            )
        )
        onSuccess()
    }
}
