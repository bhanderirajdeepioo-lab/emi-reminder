package io.helsy.emireminder.ui.screens.calculator

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.math.pow

@HiltViewModel
class CalculatorViewModel @Inject constructor() : ViewModel() {

    fun calculateEmi(principal: Double, annualRate: Double, tenureMonths: Int): Double {
        if (tenureMonths == 0 || annualRate == 0.0) return principal / maxOf(tenureMonths, 1)
        val r = annualRate / (12 * 100)
        return (principal * r * (1 + r).pow(tenureMonths)) / ((1 + r).pow(tenureMonths) - 1)
    }

    fun calculateTotalInterest(emi: Double, tenureMonths: Int, principal: Double): Double =
        (emi * tenureMonths) - principal

    fun buildAmortizationSchedule(
        principal: Double,
        annualRate: Double,
        tenureMonths: Int
    ): List<Triple<Int, Double, Double>> {
        val emi = calculateEmi(principal, annualRate, tenureMonths)
        val monthlyRate = annualRate / (12 * 100)
        var balance = principal
        return (1..tenureMonths).map { month ->
            val interest = balance * monthlyRate
            val principalPaid = emi - interest
            balance -= principalPaid
            Triple(month, interest, principalPaid)
        }
    }
}
