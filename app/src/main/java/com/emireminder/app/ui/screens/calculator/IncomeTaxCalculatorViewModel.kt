package com.emireminder.app.ui.screens.calculator

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class TaxRegime { NEW, OLD, COMPARE }

data class SlabRow(val range: String, val rate: String, val taxOnSlab: Double)

data class IncomeTaxUiState(
    val regime: TaxRegime = TaxRegime.NEW,
    val annualIncomeText: String = "1200000",
    val otherIncomeText: String = "0",
    val deduction80CText: String = "150000",
    val deduction80DText: String = "25000",
    val hraExemptionText: String = "0",
    val otherDeductionsText: String = "0",
    val newTotalTax: Double = 0.0,
    val newTaxableIncome: Double = 0.0,
    val newEffectiveTaxRate: Double = 0.0,
    val newMonthlyTds: Double = 0.0,
    val newSlabs: List<SlabRow> = emptyList(),
    val oldTotalTax: Double = 0.0,
    val oldTaxableIncome: Double = 0.0,
    val oldEffectiveTaxRate: Double = 0.0,
    val oldMonthlyTds: Double = 0.0,
    val oldSlabs: List<SlabRow> = emptyList(),
    val showInterstitialAd: Boolean = false,
    val hasShownAd: Boolean = false,
) {
    val totalIncome: Double
        get() = (annualIncomeText.toDoubleOrNull() ?: 0.0) + (otherIncomeText.toDoubleOrNull() ?: 0.0)

    val totalTax: Double
        get() = if (regime == TaxRegime.OLD) oldTotalTax else newTotalTax

    val taxableIncome: Double
        get() = if (regime == TaxRegime.OLD) oldTaxableIncome else newTaxableIncome

    val effectiveTaxRate: Double
        get() = if (regime == TaxRegime.OLD) oldEffectiveTaxRate else newEffectiveTaxRate

    val monthlyTds: Double
        get() = if (regime == TaxRegime.OLD) oldMonthlyTds else newMonthlyTds

    // Positive = New Regime is cheaper
    val savings: Double get() = oldTotalTax - newTotalTax
}

@HiltViewModel
class IncomeTaxCalculatorViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(IncomeTaxUiState().recalculated())
    val uiState: StateFlow<IncomeTaxUiState> = _uiState.asStateFlow()

    private var adJob: Job? = null
    private var adScheduled = false

    fun setRegime(regime: TaxRegime) = update { copy(regime = regime) }
    fun setIncome(text: String) = update { copy(annualIncomeText = text.filter { it.isDigit() }) }
    fun setOtherIncome(text: String) = update { copy(otherIncomeText = text.filter { it.isDigit() }) }
    fun set80C(text: String) = update { copy(deduction80CText = text.filter { it.isDigit() }) }
    fun set80D(text: String) = update { copy(deduction80DText = text.filter { it.isDigit() }) }
    fun setHra(text: String) = update { copy(hraExemptionText = text.filter { it.isDigit() }) }
    fun setOtherDeductions(text: String) = update { copy(otherDeductionsText = text.filter { it.isDigit() }) }

    fun dismissAd() {
        adJob?.cancel()
        _uiState.value = _uiState.value.copy(showInterstitialAd = false, hasShownAd = true)
    }

    private fun update(block: IncomeTaxUiState.() -> IncomeTaxUiState) {
        val next = _uiState.value.block().recalculated()
        _uiState.value = next
        // Fire interstitial once, 3s after first interaction that produces a non-zero income
        if (!adScheduled && !next.hasShownAd && next.totalIncome > 0) {
            adScheduled = true
            adJob = viewModelScope.launch {
                delay(3_000L)
                if (!_uiState.value.hasShownAd) {
                    _uiState.value = _uiState.value.copy(showInterstitialAd = true)
                }
            }
        }
    }

    private fun IncomeTaxUiState.recalculated(): IncomeTaxUiState {
        val income = (annualIncomeText.toDoubleOrNull() ?: 0.0) + (otherIncomeText.toDoubleOrNull() ?: 0.0)

        // New regime — std deduction ₹75,000; 87A rebate if income ≤ ₹7L
        val newTaxable = (income - 75_000.0).coerceAtLeast(0.0)
        val newBase = computeNewRegimeTax(newTaxable)
        val newAfterRebate = if (newTaxable <= 7_00_000.0) 0.0 else newBase
        val newTotal = newAfterRebate * 1.04

        // Old regime — std deduction ₹50,000; 80C cap ₹1.5L; 80D cap ₹1L; 87A rebate if taxable ≤ ₹5L
        val c80 = (deduction80CText.toDoubleOrNull() ?: 0.0).coerceAtMost(1_50_000.0)
        val d80 = (deduction80DText.toDoubleOrNull() ?: 0.0).coerceAtMost(1_00_000.0)
        val hra = hraExemptionText.toDoubleOrNull() ?: 0.0
        val other = otherDeductionsText.toDoubleOrNull() ?: 0.0
        val oldTaxable = (income - 50_000.0 - c80 - d80 - hra - other).coerceAtLeast(0.0)
        val oldBase = computeOldRegimeTax(oldTaxable)
        val oldAfterRebate = if (oldTaxable <= 5_00_000.0) 0.0 else oldBase
        val oldTotal = oldAfterRebate * 1.04

        return copy(
            newTotalTax = newTotal,
            newTaxableIncome = newTaxable,
            newEffectiveTaxRate = if (income > 0) newTotal / income * 100 else 0.0,
            newMonthlyTds = newTotal / 12,
            newSlabs = buildNewSlabs(newTaxable, rebate = newTaxable <= 7_00_000.0),
            oldTotalTax = oldTotal,
            oldTaxableIncome = oldTaxable,
            oldEffectiveTaxRate = if (income > 0) oldTotal / income * 100 else 0.0,
            oldMonthlyTds = oldTotal / 12,
            oldSlabs = buildOldSlabs(oldTaxable, rebate = oldTaxable <= 5_00_000.0),
        )
    }

    private fun computeNewRegimeTax(taxable: Double): Double = computeSlabTax(
        taxable,
        listOf(
            300_000.0 to 0.0,
            300_000.0 to 0.05,
            300_000.0 to 0.10,
            300_000.0 to 0.15,
            300_000.0 to 0.20,
            Double.MAX_VALUE to 0.30,
        )
    )

    private fun computeOldRegimeTax(taxable: Double): Double = computeSlabTax(
        taxable,
        listOf(
            250_000.0 to 0.0,
            250_000.0 to 0.05,
            500_000.0 to 0.20,
            Double.MAX_VALUE to 0.30,
        )
    )

    private fun computeSlabTax(taxable: Double, slabs: List<Pair<Double, Double>>): Double {
        var remaining = taxable
        var tax = 0.0
        for ((limit, rate) in slabs) {
            if (remaining <= 0) break
            val inSlab = remaining.coerceAtMost(limit)
            tax += inSlab * rate
            remaining -= inSlab
        }
        return tax
    }

    private fun buildNewSlabs(taxable: Double, rebate: Boolean): List<SlabRow> {
        val bounds = listOf(0L, 300_000L, 600_000L, 900_000L, 1_200_000L, 1_500_000L)
        val rates  = listOf(0,   5,         10,        15,        20,          30)
        val labels = listOf("₹0–₹3L", "₹3L–₹6L", "₹6L–₹9L", "₹9L–₹12L", "₹12L–₹15L", "₹15L+")
        return rates.indices.map { i ->
            val from = bounds[i].toDouble()
            val to   = if (i < bounds.lastIndex) bounds[i + 1].toDouble() else Double.MAX_VALUE
            val inSlab = (taxable - from).coerceIn(0.0, to - from)
            SlabRow(labels[i], "${rates[i]}%", if (rebate) 0.0 else inSlab * rates[i] / 100.0)
        }
    }

    private fun buildOldSlabs(taxable: Double, rebate: Boolean): List<SlabRow> {
        val bounds = listOf(0L, 250_000L, 500_000L, 1_000_000L)
        val rates  = listOf(0,  5,         20,        30)
        val labels = listOf("₹0–₹2.5L", "₹2.5L–₹5L", "₹5L–₹10L", "₹10L+")
        return rates.indices.map { i ->
            val from = bounds[i].toDouble()
            val to   = if (i < bounds.lastIndex) bounds[i + 1].toDouble() else Double.MAX_VALUE
            val inSlab = (taxable - from).coerceIn(0.0, to - from)
            SlabRow(labels[i], "${rates[i]}%", if (rebate) 0.0 else inSlab * rates[i] / 100.0)
        }
    }
}
