package io.helsy.emireminder.ui.screens.reminders

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.helsy.emireminder.data.db.entity.Loan
import io.helsy.emireminder.data.db.entity.Reminder
import io.helsy.emireminder.data.repository.LoanRepository
import io.helsy.emireminder.data.repository.ReminderRepository
import io.helsy.emireminder.notification.NotificationScheduler
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddReminderViewModel @Inject constructor(
    private val loanRepository: LoanRepository,
    private val reminderRepository: ReminderRepository,
    private val notificationScheduler: NotificationScheduler,
) : ViewModel() {

    val loans = loanRepository.getActiveLoans()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList<Loan>())

    var dueDay by mutableStateOf("1")
        private set

    var notes by mutableStateOf("")
        private set

    fun onDueDayChange(value: String) {
        val n = value.filter { it.isDigit() }
        if (n.isEmpty() || n.toIntOrNull()?.let { it in 1..31 } == true) dueDay = n
    }

    fun onNotesChange(value: String) {
        notes = value
    }

    fun resetForm() {
        dueDay = "1"
        notes = ""
    }

    fun saveReminder(
        loanId: Int,
        loanName: String,
        emiAmount: Double,
        dueDayOfMonth: Int,
        notes: String,
        onSuccess: () -> Unit,
    ) = viewModelScope.launch {
        val reminder = Reminder(
            loanId = loanId,
            loanName = loanName,
            emiAmount = emiAmount,
            dueDayOfMonth = dueDayOfMonth,
            notes = notes,
        )
        val insertedId = reminderRepository.insertReminder(reminder)
        notificationScheduler.scheduleReminder(reminder.copy(id = insertedId.toInt()))
        onSuccess()
    }
}
