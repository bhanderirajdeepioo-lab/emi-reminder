package com.emireminder.app.ui.screens.reminders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import com.emireminder.app.data.db.entity.Reminder
import com.emireminder.app.data.repository.ReminderRepository
import com.emireminder.app.notification.NotificationScheduler
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RemindersViewModel @Inject constructor(
    private val reminderRepository: ReminderRepository,
    private val notificationScheduler: NotificationScheduler,
) : ViewModel() {

    val reminders = reminderRepository.getAllReminders()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList<Reminder>())

    fun deleteReminder(reminder: Reminder) = viewModelScope.launch {
        reminderRepository.deleteReminder(reminder)
    }

    fun markAsPaid(reminder: Reminder) = viewModelScope.launch {
        reminderRepository.updateReminder(reminder.copy(isActive = false))
    }

    fun reactivate(reminder: Reminder) = viewModelScope.launch {
        reminderRepository.updateReminder(reminder.copy(isActive = true))
    }

    fun remindNow(reminder: Reminder) {
        notificationScheduler.showImmediateReminder(
            loanId   = reminder.loanId ?: reminder.id,
            loanName = reminder.loanName,
            emiAmount = reminder.emiAmount,
            dueDay   = reminder.dueDayOfMonth,
        )
    }
}
