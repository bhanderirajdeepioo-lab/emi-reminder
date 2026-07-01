package com.emireminder.app.ui.screens.reminders

import androidx.compose.runtime.derivedStateOf
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

    private var editingReminderId: Int? = null
    private var editingLoanId: Int? = null

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

    val isEditing get() = editingReminderId != null

    val isFormValid by derivedStateOf {
        loanName.isNotBlank() && emiAmount.isNotBlank() && dueDay.isNotBlank()
    }

    fun loadReminder(reminderId: Int) {
        viewModelScope.launch {
            val reminder = reminderRepository.getReminderById(reminderId) ?: return@launch
            editingReminderId = reminder.id
            editingLoanId = reminder.loanId
            loanName = reminder.loanName
            bankName = reminder.bankName
            emiAmount = reminder.emiAmount.toString()
            dueDay = reminder.dueDayOfMonth.toString()
            repeatFrequency = reminder.frequency.lowercase().replaceFirstChar { it.uppercase() }
            notes = reminder.notes
            notificationEnabled = reminder.notificationEnabled
        }
    }

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
        editingReminderId = null
        editingLoanId = null
        loanName = ""
        bankName = ""
        emiAmount = ""
        dueDay = "1"
        repeatFrequency = "Monthly"
        notes = ""
        notificationEnabled = true
        isSaving = false
    }

    fun saveReminder(onSuccess: () -> Unit) {
        if (isSaving) return
        isSaving = true
        viewModelScope.launch {
            val amount = emiAmount.toDoubleOrNull() ?: run { isSaving = false; return@launch }
            val day = dueDay.toIntOrNull() ?: run { isSaving = false; return@launch }
            val existingId = editingReminderId
            if (existingId != null) {
                val updated = Reminder(
                    id = existingId,
                    loanId = editingLoanId,
                    loanName = loanName,
                    bankName = bankName,
                    emiAmount = amount,
                    dueDayOfMonth = day,
                    frequency = repeatFrequency.uppercase(),
                    notes = notes,
                    notificationEnabled = notificationEnabled,
                )
                reminderRepository.updateReminder(updated)
                if (notificationEnabled) {
                    notificationScheduler.scheduleReminder(updated)
                } else {
                    notificationScheduler.cancelReminder(existingId)
                }
            } else {
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
            }
            isSaving = false
            onSuccess()
        }
    }
}
