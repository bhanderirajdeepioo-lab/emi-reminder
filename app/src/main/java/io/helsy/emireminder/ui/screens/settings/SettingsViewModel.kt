package io.helsy.emireminder.ui.screens.settings

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import io.helsy.emireminder.notification.NotificationScheduler
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val notificationScheduler: NotificationScheduler,
) : ViewModel() {

    fun sendTestNotification() = notificationScheduler.scheduleTestNotification()
}
