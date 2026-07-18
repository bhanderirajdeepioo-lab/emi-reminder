package com.emireminder.app.ui.screens.sms

sealed interface HistoricalScanUiState {
    data object Loading : HistoricalScanUiState
    data class Scanning(val processedCount: Int, val insertedCount: Int) : HistoricalScanUiState
    data class Complete(val insertedCount: Int, val showInterstitialAd: Boolean) : HistoricalScanUiState
    data class Error(val message: String) : HistoricalScanUiState
}
