package com.emireminder.app.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.emireminder.app.EmiApp
import com.emireminder.app.MainActivity
import com.emireminder.app.R

/**
 * Foreground service that keeps the SMS monitoring alive while the app is in the background.
 *
 * Shows a persistent low-priority notification with a "Stop Monitoring" action.
 * The service is started by [SmsMonitorController.start] and stopped either by:
 *  - The user tapping "Stop Monitoring" in the notification
 *  - [SmsMonitorController.stop] called from the settings screen
 *  - RECEIVE_SMS permission being revoked (detected in SMSReceiver)
 */
class SmsMonitorService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        startForegroundCompat()
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
    }

    private fun startForegroundCompat() {
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun buildNotification(): Notification {
        val stopPendingIntent = PendingIntent.getService(
            this,
            REQUEST_CODE_STOP,
            Intent(this, SmsMonitorService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val openAppPendingIntent = PendingIntent.getActivity(
            this,
            REQUEST_CODE_OPEN,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        return NotificationCompat.Builder(this, EmiApp.CHANNEL_SMS_MONITOR)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(getString(R.string.sms_monitor_notification_title))
            .setContentText(getString(R.string.sms_monitor_notification_body))
            .setContentIntent(openAppPendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .addAction(
                0,
                getString(R.string.sms_monitor_stop_action),
                stopPendingIntent,
            )
            .build()
    }

    companion object {
        const val ACTION_STOP = "com.emireminder.app.ACTION_STOP_SMS_MONITOR"
        private const val NOTIFICATION_ID   = 9001
        private const val REQUEST_CODE_STOP = 9002
        private const val REQUEST_CODE_OPEN = 9003
    }
}
