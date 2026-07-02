package com.emireminder.app.ui.screens.loan

import com.emireminder.app.data.db.entity.Loan
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class LoanAnalyticsViewModelTest {

    // Fixed "today" so tests are deterministic regardless of run date
    private val today = LocalDate.of(2026, 7, 1)

    @Test
    fun `empty list returns zero-value state`() {
        val state = computeLoanAnalytics(emptyList(), today)
        assertEquals(0.0, state.totalEmi, 0.0)
        assertEquals(0.0, state.totalPrincipal, 0.0)
        assertEquals(0.0, state.totalInterest, 0.0)
        assertEquals(0.0, state.totalPaid, 0.0)
        assertEquals(0.0, state.remainingInterest, 0.0)
        assertTrue(state.byCategory.isEmpty())
        assertTrue(state.loans.isEmpty())
    }

    @Test
    fun `zero interest rate loan has zero totalInterest`() {
        val loan = makeLoan(principal = 100_000.0, rate = 0.0, months = 12, emi = 8333.33)
        val state = computeLoanAnalytics(listOf(loan), today)
        assertEquals(0.0, state.totalInterest, 0.01)
    }

    @Test
    fun `totalEmi and totalPrincipal aggregate across multiple loans`() {
        val loan1 = makeLoan(principal = 100_000.0, rate = 8.0, months = 12, emi = 8696.0)
        val loan2 = makeLoan(principal = 200_000.0, rate = 10.0, months = 24, emi = 9224.0)
        val state = computeLoanAnalytics(listOf(loan1, loan2), today)
        assertEquals(300_000.0, state.totalPrincipal, 0.01)
        assertEquals(8696.0 + 9224.0, state.totalEmi, 0.01)
    }

    @Test
    fun `totalPaid is zero when loan starts today`() {
        val loan = makeLoan(principal = 100_000.0, rate = 8.0, months = 12, emi = 8696.0, startDate = today)
        val state = computeLoanAnalytics(listOf(loan), today)
        assertEquals(0.0, state.totalPaid, 0.01)
    }

    @Test
    fun `totalPaid equals emi times elapsed months`() {
        val startDate = today.minusMonths(6)
        val loan = makeLoan(principal = 100_000.0, rate = 8.0, months = 12, emi = 8696.0, startDate = startDate)
        val state = computeLoanAnalytics(listOf(loan), today)
        assertEquals(8696.0 * 6, state.totalPaid, 0.01)
    }

    @Test
    fun `byCategory groups and sorts by descending emi`() {
        val homeLoan = makeLoan(principal = 500_000.0, rate = 7.5, months = 240, emi = 4000.0, type = "HOME")
        val carLoan = makeLoan(principal = 300_000.0, rate = 9.0, months = 60, emi = 6000.0, type = "CAR")
        val state = computeLoanAnalytics(listOf(homeLoan, carLoan), today)
        assertEquals(2, state.byCategory.size)
        assertEquals("CAR", state.byCategory[0].first)
        assertEquals(6000.0, state.byCategory[0].second, 0.01)
        assertEquals("HOME", state.byCategory[1].first)
        assertEquals(4000.0, state.byCategory[1].second, 0.01)
    }

    @Test
    fun `remainingInterest is zero for loans past their tenure`() {
        val startDate = today.minusMonths(24)
        val loan = makeLoan(principal = 100_000.0, rate = 8.0, months = 12, emi = 8696.0, startDate = startDate)
        val state = computeLoanAnalytics(listOf(loan), today)
        assertEquals(0.0, state.remainingInterest, 0.01)
    }

    @Test
    fun `loans list is preserved in state`() {
        val loan = makeLoan(principal = 100_000.0, rate = 8.0, months = 12, emi = 8696.0)
        val state = computeLoanAnalytics(listOf(loan), today)
        assertEquals(1, state.loans.size)
        assertEquals(loan, state.loans[0])
    }

    private fun makeLoan(
        principal: Double,
        rate: Double,
        months: Int,
        emi: Double,
        type: String = "PERSONAL",
        startDate: LocalDate = today.minusMonths(12),
    ): Loan = Loan(
        id = 0,
        name = "Test Loan",
        type = type,
        principalAmount = principal,
        interestRate = rate,
        tenureMonths = months,
        emiAmount = emi,
        startDate = startDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
    )
}
