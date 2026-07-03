package com.emireminder.app.ui.screens.calculator

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import kotlin.math.pow

enum class InflationMode { PURCHASING_POWER, REAL_RETURNS }

enum class InflationScenario(val label: String, val rate: String) {
    EDUCATION("Education", "8.50"),
    HEALTHCARE("Healthcare", "10.00"),
    GROCERIES("Groceries", "7.00"),
    HOUSING("Housing", "6.00"),
}

data class InflationResultA(
    val currentAmount: Double,
    val inflationRate: Double,
    val years: Int,
    val futureCost: Double,
    val purchasingPowerPct: Double,
    val purchasingPowerLost: Double,
    val purchasingPowerLostPct: Double,
    val purchasingPowerValue: Double,
)

data class InflationResultB(
    val nominalRate: Double,
    val inflationRate: Double,
    val realReturn: Double,
    val beatsInflation: Boolean,
)

data class InflationUiState(
    val mode: InflationMode = InflationMode.PURCHASING_POWER,
    // Shared field
    val inflationRateText: String = "6.00",
    // Mode A — Purchasing Power
    val currentAmountText: String = "",
    val yearsText: String = "",
    // Mode B — Real Returns (nominal rate can be negative)
    val nominalRateText: String = "",
    // Scenario chip selection (null = none / custom rate)
    val selectedScenario: InflationScenario? = null,
    // Results
    val showResults: Boolean = false,
    val resultA: InflationResultA? = null,
    val resultB: InflationResultB? = null,
    // Ad state
    val triggerAd: Boolean = false,
    val adShownThisSession: Boolean = false,
) {
    val isCalculateEnabled: Boolean
        get() = when (mode) {
            InflationMode.PURCHASING_POWER ->
                currentAmountText.toDoubleOrNull()?.let { it > 0 } == true &&
                    inflationRateText.toDoubleOrNull()?.let { it > 0 } == true &&
                    yearsText.toIntOrNull()?.let { it in 1..50 } == true
            InflationMode.REAL_RETURNS ->
                nominalRateText.toDoubleOrNull() != null &&
                    inflationRateText.toDoubleOrNull()?.let { it > 0 } == true
        }
}

@HiltViewModel
class InflationCalculatorViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(InflationUiState())
    val uiState: StateFlow<InflationUiState> = _uiState.asStateFlow()

    fun setMode(mode: InflationMode) {
        _uiState.value = _uiState.value.copy(
            mode = mode,
            showResults = false,
            resultA = null,
            resultB = null,
        )
    }

    fun setInflationRateText(text: String) {
        _uiState.value = _uiState.value.copy(
            inflationRateText = text,
            selectedScenario = null,
            showResults = false,
        )
    }

    fun setInflationRateFromScenario(scenario: InflationScenario) {
        _uiState.value = _uiState.value.copy(
            inflationRateText = scenario.rate,
            selectedScenario = scenario,
            showResults = false,
        )
    }

    fun setCurrentAmountText(text: String) {
        _uiState.value = _uiState.value.copy(currentAmountText = text, showResults = false)
    }

    fun setYearsText(text: String) {
        _uiState.value = _uiState.value.copy(yearsText = text, showResults = false)
    }

    fun setNominalRateText(text: String) {
        _uiState.value = _uiState.value.copy(nominalRateText = text, showResults = false)
    }

    fun calculate() {
        val state = _uiState.value
        val inflationRate = state.inflationRateText.toDoubleOrNull() ?: return

        when (state.mode) {
            InflationMode.PURCHASING_POWER -> {
                val amount = state.currentAmountText.toDoubleOrNull() ?: return
                val years = state.yearsText.toIntOrNull() ?: return
                val r = inflationRate / 100.0
                val futureCost = amount * (1.0 + r).pow(years)
                val purchasingPowerPct = (amount / futureCost) * 100.0
                val purchasingPowerLost = futureCost - amount
                val purchasingPowerLostPct = 100.0 - purchasingPowerPct
                val purchasingPowerValue = amount / (1.0 + r).pow(years)
                _uiState.value = state.copy(
                    showResults = true,
                    resultA = InflationResultA(
                        currentAmount = amount,
                        inflationRate = inflationRate,
                        years = years,
                        futureCost = futureCost,
                        purchasingPowerPct = purchasingPowerPct,
                        purchasingPowerLost = purchasingPowerLost,
                        purchasingPowerLostPct = purchasingPowerLostPct,
                        purchasingPowerValue = purchasingPowerValue,
                    ),
                )
            }
            InflationMode.REAL_RETURNS -> {
                val nominalRate = state.nominalRateText.toDoubleOrNull() ?: return
                val n = nominalRate / 100.0
                val i = inflationRate / 100.0
                // Fisher equation: real_return = (1 + nominal) / (1 + inflation) - 1
                val realReturn = ((1.0 + n) / (1.0 + i)) - 1.0
                _uiState.value = state.copy(
                    showResults = true,
                    resultB = InflationResultB(
                        nominalRate = nominalRate,
                        inflationRate = inflationRate,
                        realReturn = realReturn * 100.0,
                        beatsInflation = realReturn > 0,
                    ),
                )
            }
        }
    }

    fun reset() {
        _uiState.value = _uiState.value.copy(showResults = false, resultA = null, resultB = null)
    }

    fun onAdTriggered() {
        _uiState.value = _uiState.value.copy(triggerAd = true)
    }

    fun onAdConsumed() {
        _uiState.value = _uiState.value.copy(triggerAd = false, adShownThisSession = true)
    }
}
