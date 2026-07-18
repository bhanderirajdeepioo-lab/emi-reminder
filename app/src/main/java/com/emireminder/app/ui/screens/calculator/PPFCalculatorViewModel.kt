package com.emireminder.app.ui.screens.calculator

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

enum class PpfExtension(val label: String, val years: Int) {
    NONE("None", 0), PLUS5("+5 Yrs", 5), PLUS10("+10 Yrs", 10)
}

data class PpfYearBreakdown(
    val year: Int,
    val openingBalance: Double,
    val deposit: Double,
    val interest: Double,
    val closingBalance: Double,
)

data class PPFUiState(
    val yearlyInvestment: Float = 1_50_000f,
    val periodYears: Int = 15,
    val extension: PpfExtension = PpfExtension.NONE,
    val interestRate: Double = 7.1,
    val interestRateText: String = "7.1",
    val maturityValue: Double = 0.0,
    val totalInvested: Double = 0.0,
    val interestEarned: Double = 0.0,
    val returnMultiple: Double = 0.0,
    val yearBreakdowns: List<PpfYearBreakdown> = emptyList(),
    val isBreakdownExpanded: Boolean = false,
    // true once the 3-second post-results delay fires (first calc per session)
    val triggerAd: Boolean = false,
    val adShownThisSession: Boolean = false,
)

private val periodOptions = listOf(15, 20, 25, 30, 35, 40, 45, 50)

@HiltViewModel
class PPFCalculatorViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(PPFUiState().recalculated())
    val uiState: StateFlow<PPFUiState> = _uiState.asStateFlow()

    fun setYearlyInvestment(v: Float) = update { copy(yearlyInvestment = v.coerceIn(500f, 1_50_000f)) }

    fun incrementPeriod() = update {
        val idx = periodOptions.indexOf(periodYears)
        copy(periodYears = if (idx < periodOptions.lastIndex) periodOptions[idx + 1] else periodYears)
    }

    fun decrementPeriod() = update {
        val idx = periodOptions.indexOf(periodYears)
        copy(periodYears = if (idx > 0) periodOptions[idx - 1] else periodYears)
    }

    fun setExtension(ext: PpfExtension) = update { copy(extension = ext) }

    fun setInterestRateText(text: String) {
        val parsed = text.toDoubleOrNull()
        _uiState.value = if (parsed != null && parsed > 0.0) {
            _uiState.value.copy(interestRateText = text, interestRate = parsed).recalculated()
        } else {
            _uiState.value.copy(interestRateText = text)
        }
    }

    fun toggleBreakdown() = update { copy(isBreakdownExpanded = !isBreakdownExpanded) }

    /** Called by the screen 3 s after results first appear, if ad not yet shown this session. */
    fun onAdTriggered() = update { copy(triggerAd = true) }

    /** Called after the interstitial ad is actually shown (or attempted). */
    fun onAdConsumed() = update { copy(triggerAd = false, adShownThisSession = true) }

    private fun update(block: PPFUiState.() -> PPFUiState) {
        _uiState.value = _uiState.value.block().recalculated()
    }

    private fun PPFUiState.recalculated(): PPFUiState {
        val totalYears = periodYears + extension.years
        val p = yearlyInvestment.toDouble()
        val r = interestRate / 100.0

        val breakdowns = ArrayList<PpfYearBreakdown>(totalYears)
        var balance = 0.0
        repeat(totalYears) { i ->
            val opening = balance
            val interest = (opening + p) * r
            balance = opening + p + interest
            breakdowns.add(
                PpfYearBreakdown(
                    year = i + 1,
                    openingBalance = opening,
                    deposit = p,
                    interest = interest,
                    closingBalance = balance,
                )
            )
        }

        val invested = p * totalYears
        val earned = balance - invested
        val multiple = if (invested > 0) balance / invested else 0.0

        return copy(
            maturityValue = balance,
            totalInvested = invested,
            interestEarned = earned,
            returnMultiple = multiple,
            yearBreakdowns = breakdowns,
        )
    }
}
