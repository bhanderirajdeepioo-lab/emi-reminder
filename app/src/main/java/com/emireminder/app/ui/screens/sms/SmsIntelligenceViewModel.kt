package com.emireminder.app.ui.screens.sms

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import com.emireminder.app.data.preferences.UserPreferencesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val NUDGE_COOLDOWN_MS = 7L * 24 * 60 * 60 * 1000

@HiltViewModel
class SmsIntelligenceViewModel @Inject constructor(
    private val prefsRepository: UserPreferencesRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<SmsIntelligenceUiState>(SmsIntelligenceUiState.Intro)
    val uiState: StateFlow<SmsIntelligenceUiState> = _uiState.asStateFlow()

    private val _showRevocationBanner = MutableStateFlow(false)
    val showRevocationBanner: StateFlow<Boolean> = _showRevocationBanner.asStateFlow()

    fun onPermissionGranted() {
        viewModelScope.launch {
            prefsRepository.setSmsIntelligenceEnabled(true)
            prefsRepository.setSmsImportEnabled(true)
            _uiState.value = SmsIntelligenceUiState.Granted
        }
    }

    fun onPermissionDenied() {
        _uiState.value = SmsIntelligenceUiState.Denied
    }

    fun dismissNudge() {
        viewModelScope.launch {
            prefsRepository.setSmsNudgeDismissedAt(System.currentTimeMillis())
        }
    }

    fun onSmsPermissionRevoked() {
        viewModelScope.launch {
            prefsRepository.setSmsIntelligenceEnabled(false)
            prefsRepository.setSmsImportEnabled(false)
            _showRevocationBanner.value = true
        }
    }

    fun dismissRevocationBanner() {
        _showRevocationBanner.value = false
    }

    suspend fun canShowNudge(): Boolean {
        val prefs = prefsRepository.userPreferences.first()
        if (prefs.smsIntelligenceEnabled) return false
        val dismissedAt = prefs.smsNudgeDismissedAt
        if (dismissedAt == 0L) return true
        return System.currentTimeMillis() - dismissedAt >= NUDGE_COOLDOWN_MS
    }
}
