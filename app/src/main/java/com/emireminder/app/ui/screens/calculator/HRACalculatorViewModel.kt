package com.emireminder.app.ui.screens.calculator

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

enum class CityType { METRO, NON_METRO }

data class HRAUiState(
    val cityType: CityType = CityType.METRO,
    val basicSalaryText: String = "50000",
    val hraReceivedText: String = "20000",
    val rentPaidText: String = "25000",
    val hraExemptionAnnual: Double = 0.0,
    val taxableHraAnnual: Double = 0.0,
    val taxSavedAnnual: Double = 0.0,
    val monthlySaving: Double = 0.0,
)

@HiltViewModel
class HRACalculatorViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(HRAUiState().recalculated())
    val uiState: StateFlow<HRAUiState> = _uiState.asStateFlow()

    fun setCityType(type: CityType) = update { copy(cityType = type) }
    fun setBasicSalary(text: String) = update { copy(basicSalaryText = text.filter { it.isDigit() }) }
    fun setHraReceived(text: String) = update { copy(hraReceivedText = text.filter { it.isDigit() }) }
    fun setRentPaid(text: String) = update { copy(rentPaidText = text.filter { it.isDigit() }) }

    private fun update(block: HRAUiState.() -> HRAUiState) {
        _uiState.value = _uiState.value.block().recalculated()
    }

    private fun HRAUiState.recalculated(): HRAUiState {
        val basic = basicSalaryText.toDoubleOrNull() ?: 0.0
        val hra = hraReceivedText.toDoubleOrNull() ?: 0.0
        val rent = rentPaidText.toDoubleOrNull() ?: 0.0
        val metroFactor = if (cityType == CityType.METRO) 0.50 else 0.40
        val rule1 = hra
        val rule2 = basic * metroFactor
        val rule3 = (rent - basic * 0.10).coerceAtLeast(0.0)
        val monthlyExemption = minOf(rule1, rule2, rule3)
        val exemptionAnnual = monthlyExemption * 12
        val hraAnnual = hra * 12
        val taxableAnnual = (hraAnnual - exemptionAnnual).coerceAtLeast(0.0)
        val taxSaved = exemptionAnnual * 0.30
        return copy(
            hraExemptionAnnual = exemptionAnnual,
            taxableHraAnnual = taxableAnnual,
            taxSavedAnnual = taxSaved,
            monthlySaving = taxSaved / 12,
        )
    }
}
