package com.emireminder.app.ui.screens.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import com.emireminder.app.data.backup.BackupRepository
import com.emireminder.app.data.backup.RestoreResult
import com.emireminder.app.data.db.entity.Loan
import com.emireminder.app.data.preferences.UserPreferences
import com.emireminder.app.data.preferences.UserPreferencesRepository
import com.emireminder.app.data.repository.LoanRepository
import com.emireminder.app.data.repository.SmsFinanceRepository
import com.emireminder.app.notification.NotificationScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface DriveBackupUiState {
    data object Idle : DriveBackupUiState
    data object BackingUp : DriveBackupUiState
    data object Restoring : DriveBackupUiState
    data class Success(val message: String) : DriveBackupUiState
    data class Error(val message: String) : DriveBackupUiState
}

sealed interface DeleteFinanceDataState {
    data object Idle : DeleteFinanceDataState
    data object Deleting : DeleteFinanceDataState
    data object Success : DeleteFinanceDataState
    data class Error(val message: String) : DeleteFinanceDataState
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val notificationScheduler: NotificationScheduler,
    private val prefsRepository: UserPreferencesRepository,
    private val loanRepository: LoanRepository,
    private val backupRepository: BackupRepository,
    private val smsFinanceRepository: SmsFinanceRepository,
) : ViewModel() {

    val prefs = prefsRepository.userPreferences
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UserPreferences())

    private val _driveBackupState = MutableStateFlow<DriveBackupUiState>(DriveBackupUiState.Idle)
    val driveBackupState = _driveBackupState.asStateFlow()

    private val _deleteFinanceDataState = MutableStateFlow<DeleteFinanceDataState>(DeleteFinanceDataState.Idle)
    val deleteFinanceDataState = _deleteFinanceDataState.asStateFlow()

    fun sendTestNotification() = notificationScheduler.scheduleTestNotification()

    fun setEmiRemindersEnabled(v: Boolean) = viewModelScope.launch {
        prefsRepository.setEmiRemindersEnabled(v)
    }

    fun setAdvanceReminderDays(days: Int) = viewModelScope.launch {
        prefsRepository.setAdvanceReminderDays(days)
    }

    fun setReminderTime(hour: Int, minute: Int) = viewModelScope.launch {
        prefsRepository.setReminderTime(hour, minute)
    }

    fun setOverdueAlertsEnabled(v: Boolean) = viewModelScope.launch {
        prefsRepository.setOverdueAlertsEnabled(v)
    }

    fun setTheme(theme: String) = viewModelScope.launch {
        prefsRepository.setTheme(theme)
    }

    fun setCurrency(currency: String) = viewModelScope.launch {
        prefsRepository.setCurrency(currency)
    }

    fun setLanguage(language: String) = viewModelScope.launch {
        prefsRepository.setLanguage(language)
    }

    fun setSmsImportEnabled(enabled: Boolean) = viewModelScope.launch {
        prefsRepository.setSmsImportEnabled(enabled)
    }

    fun setUserName(name: String) = viewModelScope.launch {
        prefsRepository.setUserName(name.trim())
    }

    suspend fun getActiveLoansForExport(): List<Loan> = loanRepository.getActiveLoansOnce()

    fun performBackup(uri: Uri) = viewModelScope.launch {
        _driveBackupState.value = DriveBackupUiState.BackingUp
        try {
            backupRepository.backup(uri)
            _driveBackupState.value = DriveBackupUiState.Success("Backup saved successfully")
        } catch (e: Exception) {
            _driveBackupState.value = DriveBackupUiState.Error(
                e.message?.take(120) ?: "Backup failed"
            )
        }
    }

    fun performRestore(uri: Uri) = viewModelScope.launch {
        _driveBackupState.value = DriveBackupUiState.Restoring
        when (val result = backupRepository.restore(uri)) {
            is RestoreResult.Success ->
                _driveBackupState.value = DriveBackupUiState.Success(
                    "Restored ${result.loanCount} loan(s) and ${result.reminderCount} reminder(s)"
                )
            is RestoreResult.Error ->
                _driveBackupState.value = DriveBackupUiState.Error(result.message)
        }
    }

    fun clearDriveBackupState() {
        _driveBackupState.value = DriveBackupUiState.Idle
    }

    fun deleteAllFinanceData() = viewModelScope.launch {
        _deleteFinanceDataState.value = DeleteFinanceDataState.Deleting
        try {
            smsFinanceRepository.deleteAllFinanceData()
            _deleteFinanceDataState.value = DeleteFinanceDataState.Success
        } catch (e: Exception) {
            _deleteFinanceDataState.value = DeleteFinanceDataState.Error(
                e.message?.take(120) ?: "Deletion failed"
            )
        }
    }

    fun clearDeleteFinanceDataState() {
        _deleteFinanceDataState.value = DeleteFinanceDataState.Idle
    }
}
