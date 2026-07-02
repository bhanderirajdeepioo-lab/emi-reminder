package com.emireminder.app.ui.screens.onboarding

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emireminder.app.data.preferences.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CountrySelectViewModel @Inject constructor(
    private val prefsRepository: UserPreferencesRepository,
) : ViewModel() {

    var currencySaved by mutableStateOf(false)
        private set

    fun saveCountryCurrency(currencyCode: String) {
        viewModelScope.launch {
            // setCurrency is a suspend fun that awaits DataStore's atomic write to disk.
            // We only set currencySaved = true AFTER the write completes, so the
            // LaunchedEffect that drives navigation never fires before the write is durable.
            prefsRepository.setCurrency(currencyCode)
            currencySaved = true
        }
    }
}
