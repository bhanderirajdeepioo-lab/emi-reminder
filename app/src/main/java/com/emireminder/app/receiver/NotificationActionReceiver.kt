package com.emireminder.app.receiver

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import dagger.hilt.android.AndroidEntryPoint
import com.emireminder.app.MainActivity
import com.emireminder.app.data.repository.LoanRepository
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
    @Inject lateinit var loanRepository: LoanRepository

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        val reminderId     = intent.getIntExtra(NotificationScheduler.EXTRA_REMINDER_ID, 0)
        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, reminderId)
        val loanId         = intent.getIntExtra(NotificationScheduler.EXTRA_LOAN_ID, -1)
        val loanName       = intent.getStringExtra(NotificationScheduler.EXTRA_LOAN_NAME) ?: ""
        val emiAmount      = intent.getDoubleExtra(NotificationScheduler.EXTRA_EMI_AMOUNT, 0.0)

        val notificationManager = context.getSystemService(NotificationManager::class.java)

        when (intent.action) {
            ACTION_PAY_NOW -> {
                val pendingResult = goAsync()
                scope.launch {
                    try {
                        val upiVpa = if (loanId != -1) {
                            loanRepository.getLoanById(loanId)?.upiVpa?.takeIf { it.isNotBlank() }
                        } else null

                        val activityIntent = if (upiVpa != null) {
                            Intent(Intent.ACTION_VIEW, Uri.parse(
                                "upi://pay?pa=$upiVpa&am=%.2f&cu=INR".format(emiAmount)
                            )).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
                        } else {
                            // No UPI VPA configured — open Loan Detail so user can set it up.
                            Intent(context, MainActivity::class.java).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                putExtra(NotificationScheduler.EXTRA_LOAN_ID, loanId)
                            }
                        }
                        context.startActivity(activityIntent)
                    } finally {
                        notificationManager.cancel(notificationId)
                        pendingResult.finish()
                    }
                }
            }
            ACTION_MARK_PAID -> {
                val pendingResult = goAsync()
                scope.launch {
                    try {
                        reminderRepository.getReminderById(reminderId)?.let { reminder ->
                            reminderRepository.updateReminder(
                                reminder.copy(lastTriggeredAt = System.currentTimeMillis()),
                            )
                        }
                    } finally {
                        notificationManager.cancel(notificationId)
                        pendingResult.finish()
                    }
                }
            }
            ACTION_SNOOZE -> {
                val snoozeTrigger = System.currentTimeMillis() + 24 * 60 * 60 * 1_000L
                notificationScheduler.scheduleSnooze(reminderId, loanName, emiAmount, snoozeTrigger)
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
