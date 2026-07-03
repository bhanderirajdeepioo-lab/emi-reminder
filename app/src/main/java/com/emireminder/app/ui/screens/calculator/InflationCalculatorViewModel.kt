package com.emireminder.app.ui.screens.calculator

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import kotlin.math.pow

enum class InflationScenario(val label: String, val rate: Float) {
    EDUCATION("Education", 10f),
    HEALTHCARE("Healthcare", 8f),
    GROCERIES("Groceries", 6f),
    HOUSING("Housing", 7f),
}

data class InflationUiState(
    val currentAmount: Float = 1_00_000f,
    val inflationRate: Float = 6f,
    val years: Int = 10,
    val activeScenario: InflationScenario? = null,
    val futureValue: Double = 0.0,
    val purchasingPowerPct: Double = 0.0,
    val extraNeeded: Double = 0.0,
)

@HiltViewModel
class InflationCalculatorViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(InflationUiState().recalculated())
    val uiState: StateFlow<InflationUiState> = _uiState.asStateFlow()

    fun setCurrentAmount(v: Float) = update { copy(currentAmount = v) }
    fun incrementRate() = update { copy(inflationRate = (inflationRate + 0.5f).coerceAtMost(20f), activeScenario = null) }
    fun decrementRate() = update { copy(inflationRate = (inflationRate - 0.5f).coerceAtLeast(1f), activeScenario = null) }
    fun incrementYears() = update { copy(years = (years + 1).coerceAtMost(50)) }
    fun decrementYears() = update { copy(years = (years - 1).coerceAtLeast(1)) }
    fun setScenario(scenario: InflationScenario) = update {
        copy(inflationRate = scenario.rate, activeScenario = scenario)
    }

    private fun update(block: InflationUiState.() -> InflationUiState) {
        _uiState.value = _uiState.value.block().recalculated()
    }

    private fun InflationUiState.recalculated(): InflationUiState {
        val p = currentAmount.toDouble()
        val r = inflationRate / 100.0
        val fv = p * (1 + r).pow(years)
        val purchasingPower = (1.0 / (1 + r).pow(years)) * 100.0
        return copy(
            futureValue = fv,
            purchasingPowerPct = purchasingPower,
            extraNeeded = fv - p,
        )
    }
}
