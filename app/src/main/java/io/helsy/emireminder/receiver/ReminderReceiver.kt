package io.helsy.emireminder.receiver

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import io.helsy.emireminder.EmiApp
import io.helsy.emireminder.MainActivity
import io.helsy.emireminder.notification.NotificationScheduler

class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val loanName   = intent.getStringExtra(NotificationScheduler.EXTRA_LOAN_NAME) ?: "EMI Due"
        val emiAmount  = intent.getDoubleExtra(NotificationScheduler.EXTRA_EMI_AMOUNT, 0.0)
        val reminderId = intent.getIntExtra(NotificationScheduler.EXTRA_REMINDER_ID, 0)

        val launchPending = PendingIntent.getActivity(
            context, reminderId,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val markPaidPending = actionPending(
            context,
            requestCode = reminderId * 10 + 1,
            action = NotificationActionReceiver.ACTION_MARK_PAID,
            reminderId = reminderId,
            notificationId = reminderId,
            loanName = loanName,
            emiAmount = emiAmount,
        )
        val snoozePending = actionPending(
            context,
            requestCode = reminderId * 10 + 2,
            action = NotificationActionReceiver.ACTION_SNOOZE,
            reminderId = reminderId,
            notificationId = reminderId,
            loanName = loanName,
            emiAmount = emiAmount,
        )

        val notification = NotificationCompat.Builder(context, EmiApp.CHANNEL_REMINDERS)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("EMI Due: $loanName")
            .setContentText("Payment of ₹%.2f is due today.".format(emiAmount))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(launchPending)
            .addAction(android.R.drawable.ic_menu_send, "Mark Paid", markPaidPending)
            .addAction(android.R.drawable.ic_media_pause, "Snooze 1 Day", snoozePending)
            .build()

        context.getSystemService(NotificationManager::class.java).notify(reminderId, notification)
    }

    private fun actionPending(
        context: Context,
        requestCode: Int,
        action: String,
        reminderId: Int,
        notificationId: Int,
        loanName: String,
        emiAmount: Double,
    ): PendingIntent {
        val intent = Intent(context, NotificationActionReceiver::class.java).apply {
            this.action = action
            putExtra(NotificationScheduler.EXTRA_REMINDER_ID, reminderId)
            putExtra(NotificationActionReceiver.EXTRA_NOTIFICATION_ID, notificationId)
            putExtra(NotificationScheduler.EXTRA_LOAN_NAME, loanName)
            putExtra(NotificationScheduler.EXTRA_EMI_AMOUNT, emiAmount)
        }
        return PendingIntent.getBroadcast(
            context, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
