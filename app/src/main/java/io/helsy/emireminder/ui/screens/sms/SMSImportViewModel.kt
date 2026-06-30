package io.helsy.emireminder.ui.screens.sms

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.helsy.emireminder.data.db.entity.SMSImport
import io.helsy.emireminder.data.repository.ReminderRepository
import io.helsy.emireminder.sms.SmsParseResult
import io.helsy.emireminder.sms.SmsScanner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface SmsImportState {
    data object Idle : SmsImportState
    data object Scanning : SmsImportState
    data class Results(
        val items: List<SmsParseResult>,
        val importedCount: Int = 0,
    ) : SmsImportState
    data class Error(val message: String) : SmsImportState
}

@HiltViewModel
class SMSImportViewModel @Inject constructor(
    private val smsScanner: SmsScanner,
    private val reminderRepository: ReminderRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<SmsImportState>(SmsImportState.Idle)
    val state: StateFlow<SmsImportState> = _state.asStateFlow()

    fun scanSmsInbox() {
        viewModelScope.launch {
            _state.value = SmsImportState.Scanning
            try {
                val results = smsScanner.scan()
                _state.value = if (results.isEmpty()) {
                    SmsImportState.Error("No EMI-related SMS messages found.")
                } else {
                    SmsImportState.Results(results)
                }
            } catch (e: SecurityException) {
                _state.value = SmsImportState.Error("SMS permission denied.")
            } catch (e: Exception) {
                _state.value = SmsImportState.Error("Failed to read SMS: ${e.message}")
            }
        }
    }

    fun importResult(result: SmsParseResult) {
        val current = _state.value as? SmsImportState.Results ?: return
        _state.value = current.copy(
            items = current.items - result,
            importedCount = current.importedCount + 1,
        )
        viewModelScope.launch {
            reminderRepository.insertSmsImport(
                SMSImport(
                    senderAddress     = result.senderAddress,
                    rawBody           = result.rawBody,
                    detectedLoanName  = result.bank.name,
                    detectedEmiAmount = result.emiAmount,
                    detectedDueDate   = "",
                    isConfirmed       = result.isConfident,
                ),
            )
        }
    }

    fun dismissResult(result: SmsParseResult) {
        val current = _state.value as? SmsImportState.Results ?: return
        _state.value = current.copy(items = current.items - result)
    }

    fun reset() { _state.value = SmsImportState.Idle }
}
