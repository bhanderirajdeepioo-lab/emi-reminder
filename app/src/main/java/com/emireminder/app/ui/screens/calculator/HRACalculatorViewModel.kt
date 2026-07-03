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
    val daText: String = "0",
    val hraReceivedText: String = "20000",
    val rentPaidText: String = "25000",
    // three Sec 10(13A) rule components (monthly)
    val component1Monthly: Double = 0.0,
    val component2Monthly: Double = 0.0,
    val component3Monthly: Double = 0.0,
    // 1, 2, or 3 indicating which rule is the binding/limiting one
    val limitingComponent: Int = 0,
    // exemption totals
    val hraExemptionMonthly: Double = 0.0,
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
    fun setDearness(text: String) = update { copy(daText = text.filter { it.isDigit() }) }
    fun setHraReceived(text: String) = update { copy(hraReceivedText = text.filter { it.isDigit() }) }
    fun setRentPaid(text: String) = update { copy(rentPaidText = text.filter { it.isDigit() }) }

    private fun update(block: HRAUiState.() -> HRAUiState) {
        _uiState.value = _uiState.value.block().recalculated()
    }

    private fun HRAUiState.recalculated(): HRAUiState {
        val basic = basicSalaryText.toDoubleOrNull() ?: 0.0
        val da = daText.toDoubleOrNull() ?: 0.0
        val basicPlusDa = basic + da
        val hra = hraReceivedText.toDoubleOrNull() ?: 0.0
        val rent = rentPaidText.toDoubleOrNull() ?: 0.0

        val metroFactor = if (cityType == CityType.METRO) 0.50 else 0.40

        // Sec 10(13A) — three statutory rule components (all monthly)
        val c1 = hra
        val c2 = basicPlusDa * metroFactor
        val c3 = (rent - basicPlusDa * 0.10).coerceAtLeast(0.0)

        val monthlyExemption = minOf(c1, c2, c3)

        // Find which component is limiting (lowest value)
        val limiting = when (monthlyExemption) {
            c1 -> 1
            c2 -> 2
            else -> 3
        }

        val exemptionAnnual = monthlyExemption * 12
        val hraAnnual = hra * 12
        val taxableAnnual = (hraAnnual - exemptionAnnual).coerceAtLeast(0.0)
        val taxSaved = exemptionAnnual * 0.30

        return copy(
            component1Monthly = c1,
            component2Monthly = c2,
            component3Monthly = c3,
            limitingComponent = limiting,
            hraExemptionMonthly = monthlyExemption,
            hraExemptionAnnual = exemptionAnnual,
            taxableHraAnnual = taxableAnnual,
            taxSavedAnnual = taxSaved,
            monthlySaving = taxSaved / 12,
        )
    }
}
