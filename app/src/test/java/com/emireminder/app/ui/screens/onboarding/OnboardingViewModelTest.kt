package com.emireminder.app.ui.screens.onboarding

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import com.emireminder.app.data.preferences.UserPreferencesRepository

@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val prefsRepository: UserPreferencesRepository = mockk(relaxed = true)

    private lateinit var viewModel: OnboardingViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        viewModel = OnboardingViewModel(prefsRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `setCurrency persists currency code to DataStore`() = runTest {
        viewModel.setCurrency("USD")
        advanceUntilIdle()

        coVerify(exactly = 1) { prefsRepository.setCurrency("USD") }
    }

    @Test
    fun `setCurrency with INR default persists INR`() = runTest {
        viewModel.setCurrency("INR")
        advanceUntilIdle()

        coVerify(exactly = 1) { prefsRepository.setCurrency("INR") }
    }

    @Test
    fun `setCurrency does not crash on DataStore write failure`() = runTest {
        coEvery { prefsRepository.setCurrency(any()) } throws RuntimeException("DataStore error")

        viewModel.setCurrency("GBP")
        advanceUntilIdle()
        // No exception propagated — ViewModel swallows write failures gracefully.
    }
}
