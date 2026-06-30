package io.helsy.emireminder.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "user_prefs")

data class UserPreferences(
    val emiRemindersEnabled: Boolean = true,
    val advanceReminderDays: Int = 3,
    val reminderTimeHour: Int = 9,
    val reminderTimeMinute: Int = 0,
    val overdueAlertsEnabled: Boolean = true,
    val theme: String = "System",
    val currency: String = "INR",
    val language: String = "English",
    val smsImportEnabled: Boolean = false,
)

@Singleton
class UserPreferencesRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private object Keys {
        val EMI_REMINDERS_ENABLED  = booleanPreferencesKey("emi_reminders_enabled")
        val ADVANCE_REMINDER_DAYS  = intPreferencesKey("advance_reminder_days")
        val REMINDER_TIME_HOUR     = intPreferencesKey("reminder_time_hour")
        val REMINDER_TIME_MINUTE   = intPreferencesKey("reminder_time_minute")
        val OVERDUE_ALERTS_ENABLED = booleanPreferencesKey("overdue_alerts_enabled")
        val THEME                  = stringPreferencesKey("theme")
        val CURRENCY               = stringPreferencesKey("currency")
        val LANGUAGE               = stringPreferencesKey("language")
        val SMS_IMPORT_ENABLED     = booleanPreferencesKey("sms_import_enabled")
    }

    val userPreferences: Flow<UserPreferences> = context.dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { prefs ->
            UserPreferences(
                emiRemindersEnabled  = prefs[Keys.EMI_REMINDERS_ENABLED]  ?: true,
                advanceReminderDays  = prefs[Keys.ADVANCE_REMINDER_DAYS]  ?: 3,
                reminderTimeHour     = prefs[Keys.REMINDER_TIME_HOUR]     ?: 9,
                reminderTimeMinute   = prefs[Keys.REMINDER_TIME_MINUTE]   ?: 0,
                overdueAlertsEnabled = prefs[Keys.OVERDUE_ALERTS_ENABLED] ?: true,
                theme                = prefs[Keys.THEME]                  ?: "System",
                currency             = prefs[Keys.CURRENCY]               ?: "INR",
                language             = prefs[Keys.LANGUAGE]               ?: "English",
                smsImportEnabled     = prefs[Keys.SMS_IMPORT_ENABLED]     ?: false,
            )
        }

    suspend fun setEmiRemindersEnabled(enabled: Boolean) =
        context.dataStore.edit { it[Keys.EMI_REMINDERS_ENABLED] = enabled }

    suspend fun setAdvanceReminderDays(days: Int) =
        context.dataStore.edit { it[Keys.ADVANCE_REMINDER_DAYS] = days }

    suspend fun setReminderTime(hour: Int, minute: Int) =
        context.dataStore.edit {
            it[Keys.REMINDER_TIME_HOUR]   = hour
            it[Keys.REMINDER_TIME_MINUTE] = minute
        }

    suspend fun setOverdueAlertsEnabled(enabled: Boolean) =
        context.dataStore.edit { it[Keys.OVERDUE_ALERTS_ENABLED] = enabled }

    suspend fun setTheme(theme: String) =
        context.dataStore.edit { it[Keys.THEME] = theme }

    suspend fun setCurrency(currency: String) =
        context.dataStore.edit { it[Keys.CURRENCY] = currency }

    suspend fun setLanguage(language: String) =
        context.dataStore.edit { it[Keys.LANGUAGE] = language }

    suspend fun setSmsImportEnabled(enabled: Boolean) =
        context.dataStore.edit { it[Keys.SMS_IMPORT_ENABLED] = enabled }
}
