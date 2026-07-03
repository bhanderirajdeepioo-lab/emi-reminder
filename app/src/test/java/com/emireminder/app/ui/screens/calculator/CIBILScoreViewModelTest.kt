package com.emireminder.app.ui.screens.calculator

import org.junit.Assert.*
import org.junit.Test

class CIBILScoreViewModelTest {

    private fun viewModel() = CIBILScoreViewModel()

    @Test
    fun `score 750 maps to Very Good band`() {
        val vm = viewModel()
        vm.setScore("750")
        assertEquals(CibilBand.VERY_GOOD, vm.uiState.value.band)
    }

    @Test
    fun `score 580 maps to Fair band`() {
        val vm = viewModel()
        vm.setScore("580")
        assertEquals(CibilBand.FAIR, vm.uiState.value.band)
    }

    @Test
    fun `score 300 maps to Poor band`() {
        val vm = viewModel()
        vm.setScore("300")
        assertEquals(CibilBand.POOR, vm.uiState.value.band)
    }

    @Test
    fun `score 900 maps to Excellent band`() {
        val vm = viewModel()
        vm.setScore("900")
        assertEquals(CibilBand.EXCELLENT, vm.uiState.value.band)
    }

    @Test
    fun `score 299 is invalid`() {
        val vm = viewModel()
        vm.setScore("299")
        assertNull(vm.uiState.value.band)
        assertNotNull(vm.uiState.value.scoreError)
    }

    @Test
    fun `score 901 is invalid`() {
        val vm = viewModel()
        vm.setScore("901")
        assertNull(vm.uiState.value.band)
        assertNotNull(vm.uiState.value.scoreError)
    }

    @Test
    fun `valid score has non-empty tips and description`() {
        val vm = viewModel()
        vm.setScore("720")
        val state = vm.uiState.value
        assertTrue(state.tips.isNotEmpty())
        assertTrue(state.description.isNotBlank())
    }

    @Test
    fun `no history mode clears score and shows guidance`() {
        val vm = viewModel()
        vm.setScore("750")
        vm.setNoHistory(true)

        val state = vm.uiState.value
        assertTrue(state.noHistory)
        assertNull(state.band)
        assertTrue(state.description.isNotBlank())
        assertTrue(state.tips.isNotEmpty())
    }

    @Test
    fun `clearing no history resets state`() {
        val vm = viewModel()
        vm.setNoHistory(true)
        vm.setNoHistory(false)

        val state = vm.uiState.value
        assertFalse(state.noHistory)
        assertNull(state.band)
        assertEquals("", state.scoreText)
    }

    @Test
    fun `band boundaries are correct`() {
        val vm = viewModel()
        mapOf(
            "549" to CibilBand.POOR,
            "550" to CibilBand.FAIR,
            "649" to CibilBand.FAIR,
            "650" to CibilBand.AVERAGE,
            "699" to CibilBand.AVERAGE,
            "700" to CibilBand.GOOD,
            "749" to CibilBand.GOOD,
            "750" to CibilBand.VERY_GOOD,
            "799" to CibilBand.VERY_GOOD,
            "800" to CibilBand.EXCELLENT,
        ).forEach { (score, expectedBand) ->
            vm.setScore(score)
            assertEquals("Score $score should be ${expectedBand.label}", expectedBand, vm.uiState.value.band)
        }
    }
}
