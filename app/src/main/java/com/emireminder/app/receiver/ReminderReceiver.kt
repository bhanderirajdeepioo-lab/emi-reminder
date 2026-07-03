package com.emireminder.app.receiver

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.emireminder.app.EmiApp
import com.emireminder.app.MainActivity
import com.emireminder.app.R
import com.emireminder.app.notification.NotificationScheduler

class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val loanName   = intent.getStringExtra(NotificationScheduler.EXTRA_LOAN_NAME) ?: "EMI Due"
        val emiAmount  = intent.getDoubleExtra(NotificationScheduler.EXTRA_EMI_AMOUNT, 0.0)
        val reminderId = intent.getIntExtra(NotificationScheduler.EXTRA_REMINDER_ID, 0)
        val loanId     = intent.getIntExtra(NotificationScheduler.EXTRA_LOAN_ID, -1)
        val upiVpa     = intent.getStringExtra(NotificationScheduler.EXTRA_UPI_VPA) ?: ""

        val launchPending = PendingIntent.getActivity(
            context, reminderId,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                // loanId lets AppNavGraph deep-link straight to Loan Detail.
                // -1 means the loan was deleted; the app opens to Home instead.
                putExtra(NotificationScheduler.EXTRA_LOAN_ID, loanId)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val markPaidPending = actionPending(
            context,
            requestCode = reminderId * 10 + 1,
            action = NotificationActionReceiver.ACTION_MARK_PAID,
            reminderId = reminderId,
            notificationId = reminderId,
            loanId = loanId,
            loanName = loanName,
            emiAmount = emiAmount,
            upiVpa = upiVpa,
        )
        val snoozePending = actionPending(
            context,
            requestCode = reminderId * 10 + 2,
            action = NotificationActionReceiver.ACTION_SNOOZE,
            reminderId = reminderId,
            notificationId = reminderId,
            loanName = loanName,
            emiAmount = emiAmount,
            upiVpa = upiVpa,
        )

        val notificationBuilder = NotificationCompat.Builder(context, EmiApp.CHANNEL_REMINDERS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("EMI Reminder")
            .setContentText("Your $loanName EMI of ₹%.2f is due today.".format(emiAmount))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(launchPending)
            .addAction(android.R.drawable.ic_menu_send, "Mark Paid", markPaidPending)
            .addAction(android.R.drawable.ic_media_pause, "Snooze 1 Day", snoozePending)

        if (upiVpa.isNotBlank()) {
            val payNowPending = actionPending(
                context,
                requestCode = reminderId * 10 + 3,
                action = NotificationActionReceiver.ACTION_PAY_NOW,
                reminderId = reminderId,
                notificationId = reminderId,
                loanId = loanId,
                loanName = loanName,
                emiAmount = emiAmount,
                upiVpa = upiVpa,
            )
            notificationBuilder.addAction(android.R.drawable.ic_menu_send, "Pay Now", payNowPending)
        }

        val notification = notificationBuilder.build()

        context.getSystemService(NotificationManager::class.java).notify(reminderId, notification)
    }

    private fun actionPending(
        context: Context,
        requestCode: Int,
        action: String,
        reminderId: Int,
        notificationId: Int,
        loanId: Int = -1,
        loanName: String,
        emiAmount: Double,
        upiVpa: String = "",
    ): PendingIntent {
        val intent = Intent(context, NotificationActionReceiver::class.java).apply {
            this.action = action
            putExtra(NotificationScheduler.EXTRA_REMINDER_ID, reminderId)
            putExtra(NotificationActionReceiver.EXTRA_NOTIFICATION_ID, notificationId)
            putExtra(NotificationScheduler.EXTRA_LOAN_ID, loanId)
            putExtra(NotificationScheduler.EXTRA_LOAN_NAME, loanName)
            putExtra(NotificationScheduler.EXTRA_EMI_AMOUNT, emiAmount)
            putExtra(NotificationScheduler.EXTRA_UPI_VPA, upiVpa)
        }
        return PendingIntent.getBroadcast(
            context, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
