package io.helsy.emireminder.receiver

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import io.helsy.emireminder.EmiApp
import io.helsy.emireminder.MainActivity

class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val loanName  = intent.getStringExtra("loan_name") ?: "EMI Due"
        val emiAmount = intent.getDoubleExtra("emi_amount", 0.0)
        val reminderId = intent.getIntExtra("reminder_id", 0)

        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, reminderId, launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, EmiApp.CHANNEL_REMINDERS)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("EMI Due: $loanName")
            .setContentText("Payment of ₹%.2f is due today.".format(emiAmount))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val manager = context.getSystemService(NotificationManager::class.java)
        manager.notify(reminderId, notification)
    }
}
