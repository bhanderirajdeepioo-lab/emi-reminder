package com.emireminder.app.ui.screens.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emireminder.app.data.preferences.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val prefsRepository: UserPreferencesRepository,
) : ViewModel() {
    fun setCurrency(currencyCode: String) = viewModelScope.launch {
        prefsRepository.setCurrency(currencyCode)
    }
}
