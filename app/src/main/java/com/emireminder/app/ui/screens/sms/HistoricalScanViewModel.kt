package com.emireminder.app.ui.screens.sms

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emireminder.app.data.preferences.UserPreferencesRepository
import com.emireminder.app.sms.HistoricalSmsScanner
import com.emireminder.app.sms.ScanProgress
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HistoricalScanViewModel @Inject constructor(
    private val scanner: HistoricalSmsScanner,
    private val prefsRepository: UserPreferencesRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<HistoricalScanUiState>(HistoricalScanUiState.Loading)
    val uiState: StateFlow<HistoricalScanUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val prefs = prefsRepository.userPreferences.first()
            val isFirstScan = !prefs.smsHistoricalScanDone
            startScan(isFirstScan = isFirstScan)
        }
    }

    private fun startScan(isFirstScan: Boolean) {
        viewModelScope.launch {
            try {
                var lastProgress = ScanProgress(0, 0)
                scanner.scan().collect { progress ->
                    lastProgress = progress
                    _uiState.value = HistoricalScanUiState.Scanning(
                        processedCount = progress.processed,
                        insertedCount  = progress.inserted,
                    )
                }
                if (isFirstScan) {
                    prefsRepository.setSmsHistoricalScanDone(true)
                }
                val showAd = isFirstScan && !prefsRepository.userPreferences.first().smsHistoricalAdShown
                _uiState.value = HistoricalScanUiState.Complete(
                    insertedCount        = lastProgress.inserted,
                    showInterstitialAd   = showAd,
                )
            } catch (e: Exception) {
                _uiState.value = HistoricalScanUiState.Error(e.message ?: "Scan failed")
            }
        }
    }

    fun onAdShown() {
        viewModelScope.launch {
            prefsRepository.setSmsHistoricalAdShown(true)
        }
    }
}
