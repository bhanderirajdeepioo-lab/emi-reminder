package com.emireminder.app.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import com.emireminder.app.data.db.entity.Reminder
import com.emireminder.app.data.preferences.UserPreferences
import com.emireminder.app.data.preferences.UserPreferencesRepository
import com.emireminder.app.receiver.ReminderReceiver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    prefsRepository: UserPreferencesRepository,
) {
    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    // Cache latest prefs on an IO-backed scope. Lazily means collection starts only on first
    // subscriber, not during Application.onCreate() Hilt singleton init (avoids startup jank).
    // SupervisorJob prevents exceptions from cancelling the scope.
    private val cachedPrefs = prefsRepository.userPreferences
        .stateIn(CoroutineScope(SupervisorJob() + Dispatchers.IO), SharingStarted.Lazily, UserPreferences())

    fun scheduleReminder(reminder: Reminder) {
        val prefs = cachedPrefs.value
        val triggerAt = nextAlarmMillis(reminder.dueDayOfMonth, prefs.reminderTimeHour, prefs.reminderTimeMinute)
        setAlarm(reminder.id, buildIntent(reminder), triggerAt)
    }

    fun cancelReminder(reminderId: Int) {
        val intent = Intent(context, ReminderReceiver::class.java)
        val pending = PendingIntent.getBroadcast(
            context, reminderId, intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
        ) ?: return
        alarmManager.cancel(pending)
    }

    fun scheduleSnooze(
        reminderId: Int,
        loanName: String,
        emiAmount: Double,
        triggerAt: Long,
    ) {
        val snoozeIntent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra(EXTRA_LOAN_NAME, loanName)
            putExtra(EXTRA_EMI_AMOUNT, emiAmount)
            putExtra(EXTRA_REMINDER_ID, reminderId)
            putExtra(EXTRA_DUE_DAY, 0)
        }
        setAlarm(reminderId, snoozeIntent, triggerAt)
    }

    fun scheduleTestNotification() {
        val triggerAt = System.currentTimeMillis() + 5_000L
        val testIntent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra(EXTRA_LOAN_NAME, "Test EMI Loan")
            putExtra(EXTRA_EMI_AMOUNT, 5000.0)
            putExtra(EXTRA_REMINDER_ID, TEST_NOTIFICATION_ID)
            putExtra(EXTRA_DUE_DAY, 0)
        }
        setAlarm(TEST_NOTIFICATION_ID, testIntent, triggerAt)
    }

    fun rescheduleAll(reminders: List<Reminder>) = reminders.forEach { scheduleReminder(it) }

    private fun setAlarm(requestCode: Int, intent: Intent, triggerAt: Long) {
        val pending = PendingIntent.getBroadcast(
            context, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
        } else {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
        }
    }

    private fun buildIntent(reminder: Reminder) = Intent(context, ReminderReceiver::class.java).apply {
        putExtra(EXTRA_LOAN_NAME, reminder.loanName)
        putExtra(EXTRA_EMI_AMOUNT, reminder.emiAmount)
        putExtra(EXTRA_REMINDER_ID, reminder.id)
        putExtra(EXTRA_DUE_DAY, reminder.dueDayOfMonth)
    }

    companion object {
        const val EXTRA_LOAN_NAME   = "loan_name"
        const val EXTRA_EMI_AMOUNT  = "emi_amount"
        const val EXTRA_REMINDER_ID = "reminder_id"
        const val EXTRA_DUE_DAY     = "due_day"
        const val TEST_NOTIFICATION_ID = Int.MAX_VALUE

        fun nextAlarmMillis(dueDayOfMonth: Int, hour: Int = 9, minute: Int = 0): Long {
            val now = Calendar.getInstance()
            val target = now.clone() as Calendar
            target.apply {
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                set(Calendar.DAY_OF_MONTH, minOf(dueDayOfMonth, getActualMaximum(Calendar.DAY_OF_MONTH)))
            }
            if (!target.after(now)) {
                target.add(Calendar.MONTH, 1)
                target.set(
                    Calendar.DAY_OF_MONTH,
                    minOf(dueDayOfMonth, target.getActualMaximum(Calendar.DAY_OF_MONTH)),
                )
            }
            return target.timeInMillis
        }
    }
}
