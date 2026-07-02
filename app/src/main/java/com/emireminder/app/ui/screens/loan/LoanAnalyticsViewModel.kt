package com.emireminder.app.ui.screens.loan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emireminder.app.data.db.entity.Loan
import com.emireminder.app.data.repository.LoanRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import kotlin.math.pow

data class LoanAnalyticsUiState(
    val loans: List<Loan> = emptyList(),
    val totalEmi: Double = 0.0,
    val totalPrincipal: Double = 0.0,
    val totalInterest: Double = 0.0,
    val totalPaid: Double = 0.0,
    val remainingInterest: Double = 0.0,
    val byCategory: List<Pair<String, Double>> = emptyList(),
)

@HiltViewModel
class LoanAnalyticsViewModel @Inject constructor(
    loanRepository: LoanRepository,
) : ViewModel() {

    val uiState = loanRepository.getActiveLoans()
        .map { loans -> computeLoanAnalytics(loans) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LoanAnalyticsUiState())
}

internal fun computeLoanAnalytics(
    loans: List<Loan>,
    today: LocalDate = LocalDate.now(),
): LoanAnalyticsUiState {
    if (loans.isEmpty()) return LoanAnalyticsUiState()

    val totalEmi = loans.sumOf { it.emiAmount }
    val totalPrincipal = loans.sumOf { it.principalAmount }

    val totalInterest = loans.sumOf { loan ->
        val r = loan.interestRate / (12 * 100)
        if (r == 0.0) 0.0 else {
            val emi = (loan.principalAmount * r * (1 + r).pow(loan.tenureMonths.toDouble())) /
                ((1 + r).pow(loan.tenureMonths.toDouble()) - 1)
            (emi * loan.tenureMonths) - loan.principalAmount
        }
    }

    val totalPaid = loans.sumOf { loan ->
        val startDate = Instant.ofEpochMilli(loan.startDate).atZone(ZoneId.systemDefault()).toLocalDate()
        val monthsElapsed = ChronoUnit.MONTHS.between(startDate, today).toInt().coerceIn(0, loan.tenureMonths)
        loan.emiAmount * monthsElapsed
    }

    val remainingInterest = loans.sumOf { loan ->
        val r = loan.interestRate / (12 * 100)
        val startDate = Instant.ofEpochMilli(loan.startDate).atZone(ZoneId.systemDefault()).toLocalDate()
        val monthsElapsed = ChronoUnit.MONTHS.between(startDate, today).toInt().coerceIn(0, loan.tenureMonths)
        if (r == 0.0 || monthsElapsed >= loan.tenureMonths) return@sumOf 0.0
        val remainingMonths = loan.tenureMonths - monthsElapsed
        var balance = loan.principalAmount
        repeat(monthsElapsed) {
            val interest = balance * r
            balance -= (loan.emiAmount - interest)
        }
        balance = maxOf(0.0, balance)
        val remainingEmi = (balance * r * (1 + r).pow(remainingMonths)) / ((1 + r).pow(remainingMonths) - 1)
        (remainingEmi * remainingMonths) - balance
    }

    val byCategory = loans.groupBy { it.type }
        .mapValues { (_, list) -> list.sumOf { it.emiAmount } }
        .entries
        .sortedByDescending { it.value }
        .map { it.key to it.value }

    return LoanAnalyticsUiState(
        loans = loans,
        totalEmi = totalEmi,
        totalPrincipal = totalPrincipal,
        totalInterest = totalInterest,
        totalPaid = totalPaid,
        remainingInterest = remainingInterest,
        byCategory = byCategory,
    )
}
