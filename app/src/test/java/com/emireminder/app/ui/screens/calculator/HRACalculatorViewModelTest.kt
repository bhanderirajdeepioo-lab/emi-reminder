package com.emireminder.app.ui.screens.calculator

import org.junit.Assert.assertEquals
import org.junit.Test

class HRACalculatorViewModelTest {

    private fun viewModel() = HRACalculatorViewModel()

    @Test
    fun `metro exemption is minimum of three components`() {
        val vm = viewModel()
        // Basic=50000, DA=0, HRA=20000, Rent=18000, Metro
        // C1=20000, C2=25000, C3=18000-5000=13000 → min=13000
        vm.setBasicSalary("50000")
        vm.setHraReceived("20000")
        vm.setRentPaid("18000")
        vm.setCityType(CityType.METRO)

        val state = vm.uiState.value
        assertEquals(13_000.0 * 12, state.hraExemptionAnnual, 1.0)
        assertEquals(7_000.0 * 12, state.taxableHraAnnual, 1.0)
        assertEquals(3, state.limitingComponent)
    }

    @Test
    fun `non-metro uses 40 percent rule for component 2`() {
        val vm = viewModel()
        // Basic=50000, DA=0, HRA=20000, Rent=25000, Non-Metro
        // C1=20000, C2=50000*0.4=20000, C3=25000-5000=20000 → min=20000
        vm.setBasicSalary("50000")
        vm.setHraReceived("20000")
        vm.setRentPaid("25000")
        vm.setCityType(CityType.NON_METRO)

        val state = vm.uiState.value
        assertEquals(20_000.0 * 12, state.hraExemptionAnnual, 1.0)
        assertEquals(0.0, state.taxableHraAnnual, 1.0)
    }

    @Test
    fun `zero rent paid makes exemption zero`() {
        val vm = viewModel()
        vm.setBasicSalary("50000")
        vm.setHraReceived("20000")
        vm.setRentPaid("0")
        vm.setCityType(CityType.METRO)

        // C3 = 0 - 5000 = max(0, -5000) = 0 → exemption=0
        val state = vm.uiState.value
        assertEquals(0.0, state.hraExemptionAnnual, 0.01)
        assertEquals(20_000.0 * 12, state.taxableHraAnnual, 1.0)
        assertEquals(3, state.limitingComponent)
    }

    @Test
    fun `component 1 is limiting when hra received is smallest`() {
        val vm = viewModel()
        // Basic=100000, HRA=5000, Rent=50000, Metro
        // C1=5000, C2=50000, C3=50000-10000=40000 → min=5000
        vm.setBasicSalary("100000")
        vm.setHraReceived("5000")
        vm.setRentPaid("50000")
        vm.setCityType(CityType.METRO)

        val state = vm.uiState.value
        assertEquals(5_000.0 * 12, state.hraExemptionAnnual, 1.0)
        assertEquals(1, state.limitingComponent)
    }

    @Test
    fun `component 2 is limiting when percentage of basic is smallest`() {
        val vm = viewModel()
        // Basic=10000, HRA=30000, Rent=50000, Metro
        // C1=30000, C2=5000, C3=50000-1000=49000 → min=5000 (C2)
        vm.setBasicSalary("10000")
        vm.setHraReceived("30000")
        vm.setRentPaid("50000")
        vm.setCityType(CityType.METRO)

        val state = vm.uiState.value
        assertEquals(5_000.0 * 12, state.hraExemptionAnnual, 1.0)
        assertEquals(2, state.limitingComponent)
    }

    @Test
    fun `annual exemption is monthly times 12`() {
        val vm = viewModel()
        vm.setBasicSalary("40000")
        vm.setHraReceived("15000")
        vm.setRentPaid("12000")
        vm.setCityType(CityType.METRO)

        val state = vm.uiState.value
        assertEquals(state.hraExemptionMonthly * 12, state.hraExemptionAnnual, 0.01)
    }

    @Test
    fun `da is included in basic plus da for rule calculations`() {
        val vm = viewModel()
        // Basic=40000, DA=10000 → Basic+DA=50000, Metro
        // HRA=20000, Rent=18000
        // C1=20000, C2=50000*0.5=25000, C3=18000-5000=13000 → min=13000
        vm.setBasicSalary("40000")
        vm.setDearness("10000")
        vm.setHraReceived("20000")
        vm.setRentPaid("18000")
        vm.setCityType(CityType.METRO)

        val state = vm.uiState.value
        assertEquals(13_000.0 * 12, state.hraExemptionAnnual, 1.0)
        assertEquals(3, state.limitingComponent)
    }

    @Test
    fun `da default zero matches basic-only calculation`() {
        val vmWithDa = viewModel().also {
            it.setBasicSalary("50000")
            it.setDearness("0")
            it.setHraReceived("20000")
            it.setRentPaid("25000")
            it.setCityType(CityType.METRO)
        }
        val vmWithout = viewModel().also {
            it.setBasicSalary("50000")
            it.setHraReceived("20000")
            it.setRentPaid("25000")
            it.setCityType(CityType.METRO)
        }
        assertEquals(vmWithout.uiState.value.hraExemptionAnnual, vmWithDa.uiState.value.hraExemptionAnnual, 0.01)
    }

    @Test
    fun `monthly saving is one twelfth of annual tax saved`() {
        val vm = viewModel()
        vm.setBasicSalary("50000")
        vm.setHraReceived("20000")
        vm.setRentPaid("25000")
        vm.setCityType(CityType.METRO)

        val state = vm.uiState.value
        assertEquals(state.taxSavedAnnual / 12, state.monthlySaving, 0.01)
    }
}
