package com.emireminder.app.ui.screens.sms

import com.emireminder.app.data.preferences.UserPreferences
import com.emireminder.app.data.preferences.UserPreferencesRepository
import com.emireminder.app.sms.HistoricalSmsScanner
import com.emireminder.app.sms.ScanProgress
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HistoricalScanViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val scanner: HistoricalSmsScanner = mockk()
    private val prefsRepository: UserPreferencesRepository = mockk(relaxed = true)

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun prefs(scanDone: Boolean = false, adShown: Boolean = false) =
        UserPreferences(smsHistoricalScanDone = scanDone, smsHistoricalAdShown = adShown)

    @Test
    fun `initial state is Loading`() {
        every { prefsRepository.userPreferences } returns flowOf(prefs())
        every { scanner.scan() } returns flowOf()
        val vm = HistoricalScanViewModel(scanner, prefsRepository)
        assertEquals(HistoricalScanUiState.Loading, vm.uiState.value)
    }

    @Test
    fun `scan emits Scanning state for each progress update`() = runTest {
        every { prefsRepository.userPreferences } returns flowOf(prefs())
        every { scanner.scan() } returns flowOf(
            ScanProgress(50, 5),
            ScanProgress(100, 12),
        )

        val vm = HistoricalScanViewModel(scanner, prefsRepository)
        advanceUntilIdle()

        val state = vm.uiState.value
        assertTrue(state is HistoricalScanUiState.Complete)
    }

    @Test
    fun `first scan marks smsHistoricalScanDone = true`() = runTest {
        every { prefsRepository.userPreferences } returns flowOf(prefs(scanDone = false, adShown = false))
        every { scanner.scan() } returns flowOf(ScanProgress(10, 2))

        val vm = HistoricalScanViewModel(scanner, prefsRepository)
        advanceUntilIdle()

        coVerify(exactly = 1) { prefsRepository.setSmsHistoricalScanDone(true) }
    }

    @Test
    fun `re-scan does not mark scanDone again`() = runTest {
        every { prefsRepository.userPreferences } returns flowOf(prefs(scanDone = true))
        every { scanner.scan() } returns flowOf(ScanProgress(10, 2))

        val vm = HistoricalScanViewModel(scanner, prefsRepository)
        advanceUntilIdle()

        coVerify(exactly = 0) { prefsRepository.setSmsHistoricalScanDone(any()) }
    }

    @Test
    fun `first scan with ad not shown sets showInterstitialAd = true`() = runTest {
        every { prefsRepository.userPreferences } returns flowOf(prefs(scanDone = false, adShown = false))
        every { scanner.scan() } returns flowOf(ScanProgress(5, 3))

        val vm = HistoricalScanViewModel(scanner, prefsRepository)
        advanceUntilIdle()

        val state = vm.uiState.value as? HistoricalScanUiState.Complete
        assertTrue(state?.showInterstitialAd == true)
    }

    @Test
    fun `re-scan does not show interstitial ad`() = runTest {
        every { prefsRepository.userPreferences } returns flowOf(prefs(scanDone = true, adShown = true))
        every { scanner.scan() } returns flowOf(ScanProgress(5, 1))

        val vm = HistoricalScanViewModel(scanner, prefsRepository)
        advanceUntilIdle()

        val state = vm.uiState.value as? HistoricalScanUiState.Complete
        assertFalse(state?.showInterstitialAd == true)
    }

    @Test
    fun `inserted count in Complete matches last progress`() = runTest {
        every { prefsRepository.userPreferences } returns flowOf(prefs())
        every { scanner.scan() } returns flowOf(
            ScanProgress(50, 7),
            ScanProgress(100, 15),
        )

        val vm = HistoricalScanViewModel(scanner, prefsRepository)
        advanceUntilIdle()

        val state = vm.uiState.value as? HistoricalScanUiState.Complete
        assertEquals(15, state?.insertedCount)
    }

    @Test
    fun `scan with zero messages completes cleanly`() = runTest {
        every { prefsRepository.userPreferences } returns flowOf(prefs())
        every { scanner.scan() } returns flowOf(ScanProgress(0, 0))

        val vm = HistoricalScanViewModel(scanner, prefsRepository)
        advanceUntilIdle()

        val state = vm.uiState.value
        assertTrue(state is HistoricalScanUiState.Complete)
        assertEquals(0, (state as HistoricalScanUiState.Complete).insertedCount)
    }

    @Test
    fun `onAdShown persists smsHistoricalAdShown = true`() = runTest {
        every { prefsRepository.userPreferences } returns flowOf(prefs())
        every { scanner.scan() } returns flowOf(ScanProgress(0, 0))

        val vm = HistoricalScanViewModel(scanner, prefsRepository)
        advanceUntilIdle()
        vm.onAdShown()
        advanceUntilIdle()

        coVerify(exactly = 1) { prefsRepository.setSmsHistoricalAdShown(true) }
    }

    @Test
    fun `scan exception transitions to Error state`() = runTest {
        every { prefsRepository.userPreferences } returns flowOf(prefs())
        every { scanner.scan() } returns kotlinx.coroutines.flow.flow {
            throw RuntimeException("ContentProvider unavailable")
        }

        val vm = HistoricalScanViewModel(scanner, prefsRepository)
        advanceUntilIdle()

        val state = vm.uiState.value
        assertTrue(state is HistoricalScanUiState.Error)
        assertEquals("ContentProvider unavailable", (state as HistoricalScanUiState.Error).message)
    }
}
