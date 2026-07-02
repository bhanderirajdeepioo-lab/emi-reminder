package com.emireminder.app.ui.screens.loan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import com.emireminder.app.data.db.entity.Loan
import com.emireminder.app.data.preferences.UserPreferencesRepository
import com.emireminder.app.data.repository.LoanRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoanDetailViewModel @Inject constructor(
    private val loanRepository: LoanRepository,
    prefsRepository: UserPreferencesRepository,
) : ViewModel() {

    val currencySymbol = prefsRepository.userPreferences
        .map { it.currencySymbol }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "₹")

    private val _loan = MutableStateFlow<Loan?>(null)
    val loan: StateFlow<Loan?> = _loan.asStateFlow()

    fun loadLoan(id: Int) = viewModelScope.launch {
        _loan.value = loanRepository.getLoanById(id)
    }

    fun deleteLoan(onComplete: () -> Unit) = viewModelScope.launch {
        _loan.value?.let { loanRepository.deleteLoan(it) }
        onComplete()
    }

    fun saveUpiVpa(vpa: String) = viewModelScope.launch {
        _loan.value?.let { current ->
            val updated = current.copy(upiVpa = vpa.trim())
            loanRepository.updateLoan(updated)
            _loan.value = updated
        }
    }
}
