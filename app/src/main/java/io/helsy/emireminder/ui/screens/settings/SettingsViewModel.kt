package io.helsy.emireminder.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.helsy.emireminder.data.preferences.UserPreferences
import io.helsy.emireminder.data.preferences.UserPreferencesRepository
import io.helsy.emireminder.notification.NotificationScheduler
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val notificationScheduler: NotificationScheduler,
    private val prefsRepository: UserPreferencesRepository,
) : ViewModel() {

    val prefs = prefsRepository.userPreferences
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UserPreferences())

    fun sendTestNotification() = notificationScheduler.scheduleTestNotification()

    fun setEmiRemindersEnabled(v: Boolean) = viewModelScope.launch {
        prefsRepository.setEmiRemindersEnabled(v)
    }

    fun setAdvanceReminderDays(days: Int) = viewModelScope.launch {
        prefsRepository.setAdvanceReminderDays(days)
    }

    fun setReminderTime(hour: Int, minute: Int) = viewModelScope.launch {
        prefsRepository.setReminderTime(hour, minute)
    }

    fun setOverdueAlertsEnabled(v: Boolean) = viewModelScope.launch {
        prefsRepository.setOverdueAlertsEnabled(v)
    }

    fun setTheme(theme: String) = viewModelScope.launch {
        prefsRepository.setTheme(theme)
    }

    fun setCurrency(currency: String) = viewModelScope.launch {
        prefsRepository.setCurrency(currency)
    }

    fun setLanguage(language: String) = viewModelScope.launch {
        prefsRepository.setLanguage(language)
    }

    fun setSmsImportEnabled(enabled: Boolean) = viewModelScope.launch {
        prefsRepository.setSmsImportEnabled(enabled)
    }
}
