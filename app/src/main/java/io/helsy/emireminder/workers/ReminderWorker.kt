package io.helsy.emireminder.workers

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import io.helsy.emireminder.EmiApp
import io.helsy.emireminder.MainActivity
import io.helsy.emireminder.data.repository.ReminderRepository

@HiltWorker
class ReminderWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val reminderRepository: ReminderRepository,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val loanName  = inputData.getString("loan_name") ?: return Result.failure()
        val emiAmount = inputData.getDouble("emi_amount", 0.0)
        val reminderId = inputData.getInt("reminder_id", 0)

        val launchIntent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext, reminderId, launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(applicationContext, EmiApp.CHANNEL_REMINDERS)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("EMI Due: $loanName")
            .setContentText("Payment of ₹%.2f is due today.".format(emiAmount))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val manager = applicationContext.getSystemService(NotificationManager::class.java)
        manager.notify(reminderId, notification)

        return Result.success()
    }
}
