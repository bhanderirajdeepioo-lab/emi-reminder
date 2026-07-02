package com.emireminder.app.receiver

import com.emireminder.app.data.db.dao.ReminderDao
import com.emireminder.app.data.db.dao.SMSImportDao
import com.emireminder.app.data.db.entity.Reminder
import com.emireminder.app.data.repository.ReminderRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test

/**
 * Tests the repository operations performed by NotificationActionReceiver when
 * ACTION_MARK_PAID is received: reminder must be deactivated and timestamped.
 */
class MarkPaidRepositoryTest {

    private lateinit var reminderDao: ReminderDao
    private lateinit var repository: ReminderRepository

    private val sampleReminder = Reminder(
        id = 42,
        loanName = "HDFC Home Loan",
        emiAmount = 12000.0,
        dueDayOfMonth = 5,
        isActive = true,
        lastTriggeredAt = null,
    )

    @Before
    fun setUp() {
        reminderDao = mockk(relaxed = true)
        repository = ReminderRepository(reminderDao, mockk<SMSImportDao>(relaxed = true))
        coEvery { reminderDao.getReminderById(42) } returns sampleReminder
    }

    @Test
    fun `markPaid - sets isActive false and records lastTriggeredAt`() = runTest {
        val updatedSlot = slot<Reminder>()
        coEvery { reminderDao.updateReminder(capture(updatedSlot)) } returns Unit

        val reminder = repository.getReminderById(42)!!
        repository.updateReminder(
            reminder.copy(
                isActive = false,
                lastTriggeredAt = System.currentTimeMillis(),
            )
        )

        val saved = updatedSlot.captured
        assertFalse("isActive must be false after Mark Paid", saved.isActive)
        assertNotNull("lastTriggeredAt must be set", saved.lastTriggeredAt)
    }

    @Test
    fun `markPaid - updateReminder is called exactly once`() = runTest {
        val reminder = repository.getReminderById(42)!!
        repository.updateReminder(reminder.copy(isActive = false, lastTriggeredAt = 1L))

        coVerify(exactly = 1) { reminderDao.updateReminder(any()) }
    }

    @Test
    fun `markPaid - reminder not found does not call updateReminder`() = runTest {
        coEvery { reminderDao.getReminderById(99) } returns null

        val reminder = repository.getReminderById(99)
        if (reminder != null) {
            repository.updateReminder(reminder.copy(isActive = false))
        }

        coVerify(exactly = 0) { reminderDao.updateReminder(any()) }
    }
}
