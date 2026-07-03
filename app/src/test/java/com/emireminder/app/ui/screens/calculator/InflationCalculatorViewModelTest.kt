package com.emireminder.app.ui.screens.calculator

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class InflationCalculatorViewModelTest {

    private fun viewModel() = InflationCalculatorViewModel()

    // ── Mode A: Purchasing Power ──────────────────────────────────────────────

    @Test
    fun `mode A calculates future cost correctly`() {
        val vm = viewModel()
        vm.setCurrentAmountText("100000")
        vm.setInflationRateText("6.00")
        vm.setYearsText("10")
        vm.calculate()

        val result = vm.uiState.value.resultA
        assertNotNull(result)
        // 1,00,000 * 1.06^10 = 1,79,084.77
        assertEquals(179_084.77, result!!.futureCost, 1.0)
    }

    @Test
    fun `mode A purchasing power pct is correct`() {
        val vm = viewModel()
        vm.setCurrentAmountText("100000")
        vm.setInflationRateText("6.00")
        vm.setYearsText("10")
        vm.calculate()

        val result = vm.uiState.value.resultA!!
        // 1/1.06^10 * 100 = 55.839...%
        assertEquals(55.84, result.purchasingPowerPct, 0.1)
        assertEquals(100.0 - result.purchasingPowerPct, result.purchasingPowerLostPct, 0.01)
    }

    @Test
    fun `mode A purchasing power lost is difference of future cost and current amount`() {
        val vm = viewModel()
        vm.setCurrentAmountText("100000")
        vm.setInflationRateText("6.00")
        vm.setYearsText("10")
        vm.calculate()

        val result = vm.uiState.value.resultA!!
        assertEquals(result.futureCost - result.currentAmount, result.purchasingPowerLost, 1.0)
    }

    @Test
    fun `mode A purchasing power value equals current amount divided by future cost factor`() {
        val vm = viewModel()
        vm.setCurrentAmountText("100000")
        vm.setInflationRateText("6.00")
        vm.setYearsText("10")
        vm.calculate()

        val result = vm.uiState.value.resultA!!
        // 1,00,000 / 1.06^10 ≈ 55,839
        assertEquals(55_839.0, result.purchasingPowerValue, 1.0)
    }

    // ── Mode B: Real Returns ──────────────────────────────────────────────────

    @Test
    fun `mode B real return uses Fisher equation`() {
        val vm = viewModel()
        vm.setMode(InflationMode.REAL_RETURNS)
        vm.setNominalRateText("12.00")
        vm.setInflationRateText("6.00")
        vm.calculate()

        val result = vm.uiState.value.resultB
        assertNotNull(result)
        // (1.12 / 1.06) - 1 = 0.05660... → 5.66%
        assertEquals(5.66, result!!.realReturn, 0.01)
        assertTrue(result.beatsInflation)
    }

    @Test
    fun `mode B returns negative real return when nominal less than inflation`() {
        val vm = viewModel()
        vm.setMode(InflationMode.REAL_RETURNS)
        vm.setNominalRateText("5.00")
        vm.setInflationRateText("6.00")
        vm.calculate()

        val result = vm.uiState.value.resultB!!
        // (1.05 / 1.06) - 1 = -0.009434... → -0.94%
        assertEquals(-0.94, result.realReturn, 0.01)
        assertFalse(result.beatsInflation)
    }

    @Test
    fun `mode B accepts negative nominal rate`() {
        val vm = viewModel()
        vm.setMode(InflationMode.REAL_RETURNS)
        vm.setNominalRateText("-5.00")
        vm.setInflationRateText("6.00")
        vm.calculate()

        val result = vm.uiState.value.resultB!!
        assertTrue(result.realReturn < 0)
        assertFalse(result.beatsInflation)
    }

    // ── Mode switch & reset ───────────────────────────────────────────────────

    @Test
    fun `switching mode resets results`() {
        val vm = viewModel()
        vm.setCurrentAmountText("100000")
        vm.setInflationRateText("6.00")
        vm.setYearsText("10")
        vm.calculate()
        assertTrue(vm.uiState.value.showResults)

        vm.setMode(InflationMode.REAL_RETURNS)

        assertFalse(vm.uiState.value.showResults)
        assertNull(vm.uiState.value.resultA)
        assertNull(vm.uiState.value.resultB)
    }

    @Test
    fun `reset clears showResults and results`() {
        val vm = viewModel()
        vm.setCurrentAmountText("100000")
        vm.setInflationRateText("6.00")
        vm.setYearsText("10")
        vm.calculate()
        vm.reset()

        assertFalse(vm.uiState.value.showResults)
        assertNull(vm.uiState.value.resultA)
    }

    // ── Calculate button enablement ───────────────────────────────────────────

    @Test
    fun `calculate disabled when amount empty`() {
        val vm = viewModel()
        vm.setInflationRateText("6.00")
        vm.setYearsText("10")

        assertFalse(vm.uiState.value.isCalculateEnabled)
    }

    @Test
    fun `calculate disabled when years out of range`() {
        val vm = viewModel()
        vm.setCurrentAmountText("100000")
        vm.setInflationRateText("6.00")
        vm.setYearsText("0")

        assertFalse(vm.uiState.value.isCalculateEnabled)
    }

    @Test
    fun `calculate enabled when mode A has all valid inputs`() {
        val vm = viewModel()
        vm.setCurrentAmountText("100000")
        vm.setInflationRateText("6.00")
        vm.setYearsText("10")

        assertTrue(vm.uiState.value.isCalculateEnabled)
    }

    @Test
    fun `calculate enabled for mode B with all valid inputs`() {
        val vm = viewModel()
        vm.setMode(InflationMode.REAL_RETURNS)
        vm.setNominalRateText("12.00")
        vm.setInflationRateText("6.00")

        assertTrue(vm.uiState.value.isCalculateEnabled)
    }

    @Test
    fun `calculate disabled for mode B when nominal rate empty`() {
        val vm = viewModel()
        vm.setMode(InflationMode.REAL_RETURNS)
        vm.setInflationRateText("6.00")

        assertFalse(vm.uiState.value.isCalculateEnabled)
    }
}
