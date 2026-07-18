package com.emireminder.app.data.repository

import com.emireminder.app.data.db.dao.MonthlyFinanceSummaryDao
import com.emireminder.app.data.db.dao.ParsedTransactionDao
import com.emireminder.app.data.db.entity.ParsedTransaction
import com.emireminder.app.domain.model.TransactionCategory
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * Integration tests for MerchantClassifier wiring into the SMS pipeline (HEL-620).
 *
 * Tests the full path: raw SMS body → SmsParser → MerchantClassifier enrichment → stored category.
 */
class SmsFinanceRepositoryTest {

    private val parsedTransactionDao: ParsedTransactionDao = mockk(relaxed = true)
    private val bankAccountRepository: BankAccountRepository = mockk(relaxed = true)
    private val monthlyFinanceSummaryDao: MonthlyFinanceSummaryDao = mockk(relaxed = true)
    private val autoDetectedEmiRepository: AutoDetectedEmiRepository = mockk(relaxed = true)

    private lateinit var repository: SmsFinanceRepository

    private val capturedEntity = slot<ParsedTransaction>()

    @Before
    fun setup() {
        coEvery { parsedTransactionDao.insert(capture(capturedEntity)) } returns 1L
        coEvery { bankAccountRepository.resolveAccount(any(), any(), any()) } returns "test-account-id"
        coEvery { monthlyFinanceSummaryDao.getByYearMonth(any()) } returns null
        repository = SmsFinanceRepository(
            parsedTransactionDao,
            bankAccountRepository,
            monthlyFinanceSummaryDao,
            autoDetectedEmiRepository,
        )
    }

    private suspend fun processAndGetCategory(body: String, sender: String = "VK-HDFCBK"): TransactionCategory? {
        val stored = repository.processIncomingSms(sender, body)
        return if (stored) capturedEntity.captured.category else null
    }

    // ── AC-1: UPI debit to Swiggy → FOOD_AND_DINING ─────────────────────────────

    @Test
    fun `AC-1 UPI debit to Swiggy VPA returns FOOD_AND_DINING`() = runTest {
        val category = processAndGetCategory(
            "Rs.350 debited from a/c XX1234 via UPI to swiggy@icici on 18-Jul-26. UPI Ref: 123456."
        )
        assertEquals(TransactionCategory.FOOD_AND_DINING, category)
    }

    // ── AC-2: UPI debit to Ola → TRANSPORT ───────────────────────────────────────

    @Test
    fun `AC-2 UPI debit to Ola VPA returns TRANSPORT`() = runTest {
        val category = processAndGetCategory(
            "Rs.150 debited from a/c XX1234 via UPI to olacabs@olamoney on 18-Jul-26. UPI Ref: 456789."
        )
        assertEquals(TransactionCategory.TRANSPORT, category)
    }

    // ── AC-3: UPI debit to Amazon → SHOPPING ─────────────────────────────────────

    @Test
    fun `AC-3 UPI debit to Amazon VPA returns SHOPPING`() = runTest {
        val category = processAndGetCategory(
            "Rs.999 debited from a/c XX1234 via UPI to amazon@apl on 18-Jul-26. UPI Ref: 789012."
        )
        assertEquals(TransactionCategory.SHOPPING, category)
    }

    // ── AC-4: UPI debit to Apollo Pharmacy → HEALTH ───────────────────────────────

    @Test
    fun `AC-4 UPI debit to Apollo Pharmacy VPA returns HEALTH`() = runTest {
        val category = processAndGetCategory(
            "Rs.450 debited from a/c XX1234 via UPI to apollopharmacy@apollo on 18-Jul-26. UPI Ref: 012345."
        )
        assertEquals(TransactionCategory.HEALTH, category)
    }

    // ── AC-5: Bank debit with "service charge" → BANK_CHARGES ────────────────────

    @Test
    fun `AC-5 bank debit with service charge returns BANK_CHARGES`() = runTest {
        val category = processAndGetCategory(
            "Rs.100 service charge debited from a/c XX1234 on 18-Jul-26. HDFC Bank."
        )
        assertEquals(TransactionCategory.BANK_CHARGES, category)
    }

    // ── AC-6: EMI debit → EMI_AND_LOANS (not overridden) ─────────────────────────

    @Test
    fun `AC-6 EMI debit is not overridden by merchant classifier`() = runTest {
        val category = processAndGetCategory(
            "Rs.5000 EMI debited from a/c XX1234 on 18-Jul-26 for loan A/c XXXX5678. HDFC Bank."
        )
        assertEquals(TransactionCategory.EMI_AND_LOANS, category)
    }

    // ── AC-7: ATM withdrawal → ATM_AND_CASH (not overridden) ─────────────────────

    @Test
    fun `AC-7 ATM withdrawal is not overridden by merchant classifier`() = runTest {
        val category = processAndGetCategory(
            "Rs.2000 ATM withdrawal from a/c XX1234 on 18-Jul-26. HDFC Bank."
        )
        assertEquals(TransactionCategory.ATM_AND_CASH, category)
    }

    // ── AC-8: Unknown UPI merchant → UNCATEGORISED ────────────────────────────────

    @Test
    fun `AC-8 UPI debit to unknown merchant stays UNCATEGORISED`() = runTest {
        val category = processAndGetCategory(
            "Rs.100 debited from a/c XX1234 via UPI to unknownshop@paytm on 18-Jul-26. UPI Ref: 999."
        )
        assertEquals(TransactionCategory.UNCATEGORISED, category)
    }
}
