package com.emireminder.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.emireminder.app.data.db.AppDatabase
import com.google.android.gms.ads.MobileAds
import com.emireminder.app.ui.ads.AppOpenAdManager
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class EmiApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    // Injecting the DB here forces Room to open the file on Dispatchers.IO during
    // Application.onCreate(), before the user can reach the Reminders tab. Without
    // this, Room opens on the first ViewModel query which can hit the main thread
    // and produce "Skipped N frames" / ANR on cold first-launch.
    @Inject
    lateinit var database: AppDatabase

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        MobileAds.initialize(this) {
            AppOpenAdManager.init(this)
        }
        createNotificationChannels()
        warmupDatabase()
    }

    private fun warmupDatabase() {
        appScope.launch {
            // Touch the DB on IO so Room creates the file and runs schema setup
            // before the first user interaction.
            database.openHelper.writableDatabase
        }
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
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_SMS_MONITOR,
                    "SMS Finance Monitor",
                    NotificationManager.IMPORTANCE_LOW
                ).apply { description = "Persistent notification while monitoring bank SMS in background" }
            )
        }
    }

    companion object {
        const val CHANNEL_REMINDERS  = "emi_reminders"
        const val CHANNEL_SMS        = "emi_sms_detected"
        const val CHANNEL_SMS_MONITOR = "emi_sms_monitor"
    }
}
