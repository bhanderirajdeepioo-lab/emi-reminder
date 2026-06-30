package io.helsy.emireminder.ui.screens.reminders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.helsy.emireminder.data.db.entity.Reminder
import io.helsy.emireminder.data.repository.ReminderRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RemindersViewModel @Inject constructor(
    private val reminderRepository: ReminderRepository,
) : ViewModel() {

    val reminders = reminderRepository.getActiveReminders()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList<Reminder>())

    fun deleteReminder(reminder: Reminder) = viewModelScope.launch {
        reminderRepository.deleteReminder(reminder)
    }
}
