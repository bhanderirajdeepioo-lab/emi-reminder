package com.emireminder.app.ui.screens.calculator

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import kotlin.math.pow

enum class FdTab { FD, RD }

enum class CompoundFreq(val label: String, val n: Int) {
    MONTHLY("Monthly", 12),
    QUARTERLY("Quarterly", 4),
    ANNUALLY("Annually", 1),
    AT_MATURITY("At Maturity", 0),
}

data class FDRDUiState(
    val selectedTab: FdTab = FdTab.FD,
    val principal: Float = 100_000f,
    val monthly: Float = 10_000f,
    val ratePercent: Float = 7.25f,
    val tenureValue: Int = 3,
    val tenureInMonths: Boolean = false,
    val compoundFreq: CompoundFreq = CompoundFreq.QUARTERLY,
    val tenureDecimal: Double = 3.0,
    val maturityValue: Double = 0.0,
    val principalForCalc: Double = 0.0,
    val interest: Double = 0.0,
    val effectiveRate: Double = 0.0,
    val bankMaturities: List<Double> = emptyList(),
)

@HiltViewModel
class FDRDCalculatorViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(FDRDUiState().recalculated())
    val uiState: StateFlow<FDRDUiState> = _uiState.asStateFlow()

    fun selectTab(tab: FdTab) = update { copy(selectedTab = tab) }
    fun setPrincipal(v: Float) = update { copy(principal = v) }
    fun setMonthly(v: Float) = update { copy(monthly = v) }
    fun setRate(v: Float) = update { copy(ratePercent = v) }
    fun setTenureValue(v: Int) = update { copy(tenureValue = v) }
    fun setTenureInMonths(v: Boolean) = update { copy(tenureInMonths = v) }
    fun setCompoundFreq(v: CompoundFreq) = update { copy(compoundFreq = v) }

    private fun update(block: FDRDUiState.() -> FDRDUiState) {
        _uiState.value = _uiState.value.block().recalculated()
    }

    private fun FDRDUiState.recalculated(): FDRDUiState {
        val dec = if (tenureInMonths) tenureValue / 12.0 else tenureValue.toDouble()
        val maturity = if (selectedTab == FdTab.FD)
            calcFD(principal.toDouble(), ratePercent.toDouble(), dec, compoundFreq)
        else
            calcRD(monthly.toDouble(), ratePercent.toDouble(), dec, compoundFreq)
        val princForCalc = if (selectedTab == FdTab.FD) principal.toDouble()
        else monthly * (tenureValue * if (tenureInMonths) 1 else 12).toDouble()
        val intr = maturity - princForCalc
        val effRate = if (dec > 0 && princForCalc > 0) ((maturity / princForCalc - 1) / dec * 100) else 0.0
        val bankMats = FD_BANK_RATES.map { entry ->
            calcFD(principal.toDouble(), entry.ratePercent, dec, compoundFreq)
        }
        return copy(
            tenureDecimal = dec,
            maturityValue = maturity,
            principalForCalc = princForCalc,
            interest = intr,
            effectiveRate = effRate,
            bankMaturities = bankMats,
        )
    }
}

private fun calcFD(principal: Double, ratePercent: Double, tenureYears: Double, freq: CompoundFreq): Double {
    val r = ratePercent / 100.0
    return if (freq == CompoundFreq.AT_MATURITY) {
        principal * (1 + r * tenureYears)
    } else {
        val n = freq.n.toDouble()
        principal * (1 + r / n).pow(n * tenureYears)
    }
}

private fun calcRD(monthly: Double, ratePercent: Double, tenureYears: Double, freq: CompoundFreq): Double {
    val r = ratePercent / 100.0
    val totalMonths = (tenureYears * 12).toInt()
    var maturity = 0.0
    if (freq == CompoundFreq.AT_MATURITY) {
        for (m in 1..totalMonths) {
            val remainingYears = (totalMonths - m + 1) / 12.0
            maturity += monthly * (1 + r * remainingYears)
        }
    } else {
        val n = freq.n.toDouble()
        val rPerPeriod = r / n
        val monthsPerPeriod = 12.0 / n
        for (m in 1..totalMonths) {
            val periodsRemaining = (totalMonths - m + 1).toDouble() / monthsPerPeriod
            maturity += monthly * (1 + rPerPeriod).pow(periodsRemaining)
        }
    }
    return maturity
}
