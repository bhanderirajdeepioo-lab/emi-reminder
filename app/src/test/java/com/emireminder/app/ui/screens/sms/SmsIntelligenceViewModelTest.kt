package com.emireminder.app.ui.screens.sms

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
import com.emireminder.app.data.preferences.UserPreferences
import com.emireminder.app.data.preferences.UserPreferencesRepository

@OptIn(ExperimentalCoroutinesApi::class)
class SmsIntelligenceViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val prefsRepository: UserPreferencesRepository = mockk(relaxed = true)

    private lateinit var viewModel: SmsIntelligenceViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        every { prefsRepository.userPreferences } returns flowOf(UserPreferences())
        viewModel = SmsIntelligenceViewModel(prefsRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial uiState is Intro`() {
        assertEquals(SmsIntelligenceUiState.Intro, viewModel.uiState.value)
    }

    @Test
    fun `onPermissionGranted sets state to Granted and persists to prefs`() = runTest {
        viewModel.onPermissionGranted()
        advanceUntilIdle()

        assertEquals(SmsIntelligenceUiState.Granted, viewModel.uiState.value)
        coVerify(exactly = 1) { prefsRepository.setSmsIntelligenceEnabled(true) }
        coVerify(exactly = 1) { prefsRepository.setSmsImportEnabled(true) }
    }

    @Test
    fun `onPermissionDenied sets state to Denied`() {
        viewModel.onPermissionDenied()

        assertEquals(SmsIntelligenceUiState.Denied, viewModel.uiState.value)
    }

    @Test
    fun `dismissNudge persists current timestamp to prefs`() = runTest {
        viewModel.dismissNudge()
        advanceUntilIdle()

        coVerify(exactly = 1) { prefsRepository.setSmsNudgeDismissedAt(any()) }
    }

    @Test
    fun `onSmsPermissionRevoked disables intelligence and shows banner`() = runTest {
        viewModel.onSmsPermissionRevoked()
        advanceUntilIdle()

        coVerify(exactly = 1) { prefsRepository.setSmsIntelligenceEnabled(false) }
        coVerify(exactly = 1) { prefsRepository.setSmsImportEnabled(false) }
        assertTrue(viewModel.showRevocationBanner.value)
    }

    @Test
    fun `dismissRevocationBanner hides banner`() = runTest {
        viewModel.onSmsPermissionRevoked()
        advanceUntilIdle()

        viewModel.dismissRevocationBanner()

        assertFalse(viewModel.showRevocationBanner.value)
    }

    @Test
    fun `canShowNudge returns true when never dismissed`() = runTest {
        every { prefsRepository.userPreferences } returns flowOf(
            UserPreferences(smsIntelligenceEnabled = false, smsNudgeDismissedAt = 0L)
        )
        viewModel = SmsIntelligenceViewModel(prefsRepository)

        assertTrue(viewModel.canShowNudge())
    }

    @Test
    fun `canShowNudge returns false when intelligence already enabled`() = runTest {
        every { prefsRepository.userPreferences } returns flowOf(
            UserPreferences(smsIntelligenceEnabled = true, smsNudgeDismissedAt = 0L)
        )
        viewModel = SmsIntelligenceViewModel(prefsRepository)

        assertFalse(viewModel.canShowNudge())
    }

    @Test
    fun `canShowNudge returns false within 7-day cooldown`() = runTest {
        val recentDismissal = System.currentTimeMillis() - (3L * 24 * 60 * 60 * 1000)
        every { prefsRepository.userPreferences } returns flowOf(
            UserPreferences(smsIntelligenceEnabled = false, smsNudgeDismissedAt = recentDismissal)
        )
        viewModel = SmsIntelligenceViewModel(prefsRepository)

        assertFalse(viewModel.canShowNudge())
    }

    @Test
    fun `canShowNudge returns true after 7-day cooldown`() = runTest {
        val oldDismissal = System.currentTimeMillis() - (8L * 24 * 60 * 60 * 1000)
        every { prefsRepository.userPreferences } returns flowOf(
            UserPreferences(smsIntelligenceEnabled = false, smsNudgeDismissedAt = oldDismissal)
        )
        viewModel = SmsIntelligenceViewModel(prefsRepository)

        assertTrue(viewModel.canShowNudge())
    }
}
