package com.emireminder.app.ui.screens.reminders

import io.mockk.coEvery
import io.mockk.mockk
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertNull
import junit.framework.TestCase.assertTrue
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
import com.emireminder.app.data.repository.ReminderRepository
import com.emireminder.app.notification.NotificationScheduler

@OptIn(ExperimentalCoroutinesApi::class)
class AddReminderViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val repository: ReminderRepository = mockk()
    private val scheduler: NotificationScheduler = mockk(relaxed = true)

    private lateinit var viewModel: AddReminderViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        viewModel = AddReminderViewModel(repository, scheduler)
        viewModel.onLoanNameChange("Test Loan")
        viewModel.onEmiAmountChange("5000")
        viewModel.onDueDayChange("10")
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `saveReminder on DB error resets isSaving and exposes error message`() = runTest {
        coEvery { repository.insertReminder(any()) } throws RuntimeException("DB error")

        var successCalled = false
        viewModel.saveReminder { successCalled = true }
        advanceUntilIdle()

        assertFalse(viewModel.isSaving)
        assertFalse(successCalled)
        assertEquals("Failed to save reminder. Please try again.", viewModel.errorMessage)
    }

    @Test
    fun `saveReminder on success invokes onSuccess and leaves errorMessage null`() = runTest {
        coEvery { repository.insertReminder(any()) } returns 1L

        var successCalled = false
        viewModel.saveReminder { successCalled = true }
        advanceUntilIdle()

        assertFalse(viewModel.isSaving)
        assertTrue(successCalled)
        assertNull(viewModel.errorMessage)
    }

    @Test
    fun `resetForm clears errorMessage`() = runTest {
        coEvery { repository.insertReminder(any()) } throws RuntimeException("DB error")
        viewModel.saveReminder {}
        advanceUntilIdle()

        viewModel.resetForm()

        assertNull(viewModel.errorMessage)
    }
}
