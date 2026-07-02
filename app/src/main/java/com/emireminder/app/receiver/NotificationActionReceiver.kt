package com.emireminder.app.receiver

import android.app.NotificationManager
import android.content.ActivityNotFoundException
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import dagger.hilt.android.AndroidEntryPoint
import com.emireminder.app.R
import com.emireminder.app.data.repository.ReminderRepository
import com.emireminder.app.notification.NotificationScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class NotificationActionReceiver : BroadcastReceiver() {

    @Inject lateinit var reminderRepository: ReminderRepository
    @Inject lateinit var notificationScheduler: NotificationScheduler

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        val reminderId     = intent.getIntExtra(NotificationScheduler.EXTRA_REMINDER_ID, 0)
        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, reminderId)
        val loanId         = intent.getIntExtra(NotificationScheduler.EXTRA_LOAN_ID, -1)
        val loanName       = intent.getStringExtra(NotificationScheduler.EXTRA_LOAN_NAME) ?: ""
        val emiAmount      = intent.getDoubleExtra(NotificationScheduler.EXTRA_EMI_AMOUNT, 0.0)
        val upiVpa         = intent.getStringExtra(NotificationScheduler.EXTRA_UPI_VPA) ?: ""

        val notificationManager = context.getSystemService(NotificationManager::class.java)

        when (intent.action) {
            ACTION_PAY_NOW -> {
                val uriString = "upi://pay?pa=%s&pn=%s&am=%.2f&cu=INR&tn=%s".format(
                    Uri.encode(upiVpa),
                    Uri.encode(loanName),
                    emiAmount,
                    Uri.encode("EMI Payment"),
                )
                val payIntent = Intent(Intent.ACTION_VIEW, Uri.parse(uriString)).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                try {
                    context.startActivity(payIntent)
                } catch (e: ActivityNotFoundException) {
                    Toast.makeText(
                        context,
                        context.getString(R.string.no_upi_app_found),
                        Toast.LENGTH_SHORT,
                    ).show()
                }
                notificationManager.cancel(notificationId)
            }
            ACTION_MARK_PAID -> {
                val pendingResult = goAsync()
                scope.launch {
                    try {
                        reminderRepository.getReminderById(reminderId)?.let { reminder ->
                            reminderRepository.updateReminder(
                                reminder.copy(
                                    isActive = false,
                                    lastTriggeredAt = System.currentTimeMillis(),
                                ),
                            )
                            notificationScheduler.cancelReminder(reminderId)
                        }
                    } finally {
                        notificationManager.cancel(notificationId)
                        pendingResult.finish()
                    }
                }
            }
            ACTION_SNOOZE -> {
                val snoozeTrigger = System.currentTimeMillis() + 24 * 60 * 60 * 1_000L
                notificationScheduler.scheduleSnooze(reminderId, loanName, emiAmount, snoozeTrigger, upiVpa)
                notificationManager.cancel(notificationId)
            }
        }
    }

    companion object {
        const val ACTION_PAY_NOW        = "com.emireminder.app.ACTION_PAY_NOW"
        const val ACTION_MARK_PAID      = "com.emireminder.app.ACTION_MARK_PAID"
        const val ACTION_SNOOZE         = "com.emireminder.app.ACTION_SNOOZE"
        const val EXTRA_NOTIFICATION_ID = "notification_id"
    }
}
