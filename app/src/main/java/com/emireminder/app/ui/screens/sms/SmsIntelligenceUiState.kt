package com.emireminder.app.ui.screens.sms

sealed interface SmsIntelligenceUiState {
    data object Intro : SmsIntelligenceUiState
    data object Requesting : SmsIntelligenceUiState
    data object Denied : SmsIntelligenceUiState
    data object Granted : SmsIntelligenceUiState
}
