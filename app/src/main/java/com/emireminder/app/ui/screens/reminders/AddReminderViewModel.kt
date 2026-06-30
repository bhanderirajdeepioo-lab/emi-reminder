package com.emireminder.app.ui.screens.reminders

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import com.emireminder.app.data.db.entity.Reminder
import com.emireminder.app.data.repository.ReminderRepository
import com.emireminder.app.notification.NotificationScheduler
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddReminderViewModel @Inject constructor(
    private val reminderRepository: ReminderRepository,
    private val notificationScheduler: NotificationScheduler,
) : ViewModel() {

    var loanName by mutableStateOf("")
        private set
    var bankName by mutableStateOf("")
        private set
    var emiAmount by mutableStateOf("")
        private set
    var dueDay by mutableStateOf("1")
        private set
    var repeatFrequency by mutableStateOf("Monthly")
        private set
    var notes by mutableStateOf("")
        private set
    var notificationEnabled by mutableStateOf(true)
        private set
    var isSaving by mutableStateOf(false)
        private set

    val isFormValid: Boolean
        get() = loanName.isNotBlank() && emiAmount.isNotBlank() && dueDay.isNotBlank()

    fun onLoanNameChange(v: String) { loanName = v }
    fun onBankNameChange(v: String) { bankName = v }
    fun onEmiAmountChange(v: String) {
        if (v.isEmpty() || v.matches(Regex("\\d*\\.?\\d*"))) emiAmount = v
    }
    fun onDueDayChange(value: String) {
        val n = value.filter { it.isDigit() }
        if (n.isEmpty() || n.toIntOrNull()?.let { it in 1..31 } == true) dueDay = n
    }
    fun onRepeatFrequencyChange(v: String) { repeatFrequency = v }
    fun onNotesChange(value: String) { notes = value }
    fun onNotificationToggle() { notificationEnabled = !notificationEnabled }

    fun resetForm() {
        loanName = ""
        bankName = ""
        emiAmount = ""
        dueDay = "1"
        repeatFrequency = "Monthly"
        notes = ""
        notificationEnabled = true
        isSaving = false
    }

    fun saveReminder(onSuccess: () -> Unit) = viewModelScope.launch {
        val amount = emiAmount.toDoubleOrNull() ?: return@launch
        val day = dueDay.toIntOrNull() ?: return@launch
        isSaving = true
        val reminder = Reminder(
            loanName = loanName,
            bankName = bankName,
            emiAmount = amount,
            dueDayOfMonth = day,
            frequency = repeatFrequency.uppercase(),
            notes = notes,
            notificationEnabled = notificationEnabled,
        )
        val insertedId = reminderRepository.insertReminder(reminder)
        if (notificationEnabled) {
            notificationScheduler.scheduleReminder(reminder.copy(id = insertedId.toInt()))
        }
        isSaving = false
        onSuccess()
    }
}
