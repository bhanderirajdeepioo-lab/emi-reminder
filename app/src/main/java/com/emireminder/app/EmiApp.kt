package com.emireminder.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class EmiApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_REMINDERS,
                    "EMI Reminders",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply { description = "EMI payment due reminders" }
            )
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_SMS,
                    "SMS Loan Detected",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply { description = "Notifications when a loan EMI is detected via SMS" }
            )
        }
    }

    companion object {
        const val CHANNEL_REMINDERS = "emi_reminders"
        const val CHANNEL_SMS = "emi_sms_detected"
    }
}
