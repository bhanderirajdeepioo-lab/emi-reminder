package com.emireminder.app.ui.screens.calculator

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

enum class TaxRegime { NEW, OLD }

data class IncomeTaxUiState(
    val regime: TaxRegime = TaxRegime.NEW,
    val annualIncomeText: String = "1200000",
    val deduction80CText: String = "150000",
    val deduction80DText: String = "25000",
    val standardDeduction: Double = 75_000.0,
    val totalTax: Double = 0.0,
    val taxableIncome: Double = 0.0,
    val effectiveTaxRate: Double = 0.0,
    val monthlyTds: Double = 0.0,
)

@HiltViewModel
class IncomeTaxCalculatorViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(IncomeTaxUiState().recalculated())
    val uiState: StateFlow<IncomeTaxUiState> = _uiState.asStateFlow()

    fun setRegime(regime: TaxRegime) = update { copy(regime = regime) }
    fun setIncome(text: String) = update { copy(annualIncomeText = text.filter { it.isDigit() }) }
    fun set80C(text: String) = update { copy(deduction80CText = text.filter { it.isDigit() }) }
    fun set80D(text: String) = update { copy(deduction80DText = text.filter { it.isDigit() }) }

    private fun update(block: IncomeTaxUiState.() -> IncomeTaxUiState) {
        _uiState.value = _uiState.value.block().recalculated()
    }

    private fun IncomeTaxUiState.recalculated(): IncomeTaxUiState {
        val income = annualIncomeText.toDoubleOrNull() ?: 0.0
        val stdDeduction = if (regime == TaxRegime.NEW) 75_000.0 else 50_000.0
        val c80 = if (regime == TaxRegime.OLD) (deduction80CText.toDoubleOrNull() ?: 0.0).coerceAtMost(1_50_000.0) else 0.0
        val d80 = if (regime == TaxRegime.OLD) (deduction80DText.toDoubleOrNull() ?: 0.0).coerceAtMost(1_00_000.0) else 0.0
        val taxable = (income - stdDeduction - c80 - d80).coerceAtLeast(0.0)
        val baseTax = if (regime == TaxRegime.NEW) computeNewRegimeTax(taxable) else computeOldRegimeTax(taxable)
        val cess = baseTax * 0.04
        val total = baseTax + cess
        val effectiveRate = if (income > 0) total / income * 100 else 0.0
        return copy(
            standardDeduction = stdDeduction,
            totalTax = total,
            taxableIncome = taxable,
            effectiveTaxRate = effectiveRate,
            monthlyTds = total / 12,
        )
    }

    private fun computeNewRegimeTax(taxable: Double): Double {
        // FY 2024-25 new regime slabs (rebate u/s 87A: income ≤7L→ tax=0 handled at UI layer via note)
        val slabs = listOf(
            300_000.0 to 0.0,
            300_000.0 to 0.05,
            300_000.0 to 0.10,
            300_000.0 to 0.15,
            300_000.0 to 0.20,
            Double.MAX_VALUE to 0.30,
        )
        return computeSlabTax(taxable, slabs)
    }

    private fun computeOldRegimeTax(taxable: Double): Double {
        val slabs = listOf(
            250_000.0 to 0.0,
            250_000.0 to 0.05,
            500_000.0 to 0.20,
            Double.MAX_VALUE to 0.30,
        )
        return computeSlabTax(taxable, slabs)
    }

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
}
