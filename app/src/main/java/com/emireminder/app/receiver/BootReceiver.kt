package com.emireminder.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import com.emireminder.app.data.preferences.UserPreferencesRepository
import com.emireminder.app.data.repository.ReminderRepository
import com.emireminder.app.notification.NotificationScheduler
import com.emireminder.app.service.SmsMonitorController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject lateinit var reminderRepository: ReminderRepository
    @Inject lateinit var notificationScheduler: NotificationScheduler
    @Inject lateinit var prefsRepository: UserPreferencesRepository

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val prefs = prefsRepository.userPreferences.first()
                val active = reminderRepository.getActiveReminders().first()
                notificationScheduler.rescheduleAll(active)
                if (prefs.smsIntelligenceEnabled) {
                    SmsMonitorController.start(context)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
