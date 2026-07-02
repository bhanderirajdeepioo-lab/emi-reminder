package com.emireminder.app.ui.screens.calculator

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import kotlin.math.pow

enum class SipTab(val label: String) { REGULAR("Regular SIP"), STEPUP("Step-up SIP"), LUMPSUM("Lump Sum") }

data class SIPUiState(
    val selectedTab: SipTab = SipTab.REGULAR,
    val monthlyAmount: Float = 10_000f,
    val lumpSumAmount: Float = 1_00_000f,
    val annualRate: Float = 12f,
    val tenureYears: Int = 15,
    val totalMonths: Int = 180,
    val corpus: Double = 0.0,
    val invested: Double = 0.0,
    val returns: Double = 0.0,
    val returnsPercent: Double = 0.0,
    val chartPoints: List<Pair<Double, Double>> = emptyList(),
)

@HiltViewModel
class SIPCalculatorViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(SIPUiState().recalculated())
    val uiState: StateFlow<SIPUiState> = _uiState.asStateFlow()

    fun selectTab(tab: SipTab) = update { copy(selectedTab = tab) }
    fun setMonthlyAmount(v: Float) = update { copy(monthlyAmount = v) }
    fun setLumpSumAmount(v: Float) = update { copy(lumpSumAmount = v) }
    fun setAnnualRate(v: Float) = update { copy(annualRate = v) }
    fun setTenureYears(v: Int) = update { copy(tenureYears = v) }

    private fun update(block: SIPUiState.() -> SIPUiState) {
        _uiState.value = _uiState.value.block().recalculated()
    }

    private fun SIPUiState.recalculated(): SIPUiState {
        val months = tenureYears * 12
        val c = when (selectedTab) {
            SipTab.REGULAR -> calcSIPCorpus(monthlyAmount.toDouble(), annualRate.toDouble(), months)
            SipTab.STEPUP  -> calcStepUpSIPCorpus(monthlyAmount.toDouble(), annualRate.toDouble(), tenureYears, 10.0)
            SipTab.LUMPSUM -> calcLumpSum(lumpSumAmount.toDouble(), annualRate.toDouble(), tenureYears)
        }
        val inv = when (selectedTab) {
            SipTab.REGULAR -> monthlyAmount.toDouble() * months
            SipTab.STEPUP  -> {
                var m = monthlyAmount.toDouble(); var t = 0.0
                for (y in 0 until tenureYears) { t += m * 12; m *= 1.10 }
                t
            }
            SipTab.LUMPSUM -> lumpSumAmount.toDouble()
        }
        val ret = c - inv
        val retPct = if (inv > 0) (ret / inv * 100) else 0.0
        val pts = (1..tenureYears).map { yr ->
            val yrMonths = yr * 12
            val yrCorpus = when (selectedTab) {
                SipTab.REGULAR -> calcSIPCorpus(monthlyAmount.toDouble(), annualRate.toDouble(), yrMonths)
                SipTab.STEPUP  -> calcStepUpSIPCorpus(monthlyAmount.toDouble(), annualRate.toDouble(), yr, 10.0)
                SipTab.LUMPSUM -> calcLumpSum(lumpSumAmount.toDouble(), annualRate.toDouble(), yr)
            }
            val yrInv = when (selectedTab) {
                SipTab.REGULAR -> monthlyAmount.toDouble() * yrMonths
                SipTab.STEPUP  -> {
                    var m2 = monthlyAmount.toDouble(); var t2 = 0.0
                    for (y in 0 until yr) { t2 += m2 * 12; m2 *= 1.10 }
                    t2
                }
                SipTab.LUMPSUM -> lumpSumAmount.toDouble()
            }
            Pair(yrCorpus, yrInv)
        }
        return copy(
            totalMonths = months,
            corpus = c,
            invested = inv,
            returns = ret,
            returnsPercent = retPct,
            chartPoints = pts,
        )
    }
}

private fun calcSIPCorpus(monthly: Double, annualRate: Double, months: Int): Double {
    val r = annualRate / 100.0 / 12.0
    if (r == 0.0) return monthly * months
    return monthly * ((1 + r).pow(months) - 1) / r * (1 + r)
}

private fun calcLumpSum(principal: Double, annualRate: Double, years: Int): Double {
    val r = annualRate / 100.0
    return principal * (1 + r).pow(years)
}

private fun calcStepUpSIPCorpus(monthly: Double, annualRate: Double, years: Int, stepUpPercent: Double): Double {
    val r = annualRate / 100.0 / 12.0
    var total = 0.0
    var currentMonthly = monthly
    val totalMonths = years * 12
    for (month in 1..totalMonths) {
        if (month > 1 && (month - 1) % 12 == 0) currentMonthly *= (1 + stepUpPercent / 100.0)
        val remainingMonths = totalMonths - month + 1
        total += currentMonthly * (1 + r).pow(remainingMonths)
    }
    return total
}
