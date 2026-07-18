package com.emireminder.app.sms

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [SmsParser.parseTransaction] covering PRD §5.1 examples and all ACs.
 *
 * AC #1  — All 8 PRD §5.1 SMS examples parse to the right category / amount / date / account
 * AC #2  — OTP messages return null; no transaction record created
 * AC #3  — Confidence ≤ 50 returns a ParsedTransaction (for analytics) with isImportEligible=false
 * AC #4  — Unknown sender ID is ignored completely (null)
 * AC #5  — Unit test per classification rule
 * AC #6  — VPA merchant name extracted from UPI SMS
 * AC #7  — Salary credit identified with irregular (compact) date format
 * AC #8  — Amount parser handles commas (₹8,750 = 8750.0) and decimals (₹2,500.00 = 2500.0)
 */
class ParsedTransactionTest {

    // ─────────────────────────────────────────────────────────────────────────
    // AC #1 — PRD §5.1 Examples (one per category)
    // ─────────────────────────────────────────────────────────────────────────

    /** PRD §5.1 example 1 — EMI Debit (HDFC) */
    @Test fun prd_example1_emiDebit_hdfc() {
        val result = SmsParser.parseTransaction(
            "HDFCBK",
            "HDFC Bank: EMI of Rs.8,750.00 debited from your a/c XX1234 for Home Loan on 01-Jul-26. Avl Bal: Rs.42,300.00",
        )
        assertNotNull(result)
        assertEquals(TransactionCategory.EMI_DEBIT, result!!.category)
        assertEquals(8750.0, result.amount, 0.01)
        assertEquals("1234", result.accountLast4)
        assertNotNull("date should be extracted", result.date)
        assertTrue("confidence should be ≥90 for known sender+amount+date", result.confidenceScore >= 90)
    }

    /** PRD §5.1 example 2 — Salary Credit (SBI) */
    @Test fun prd_example2_salaryCredit_sbi() {
        val result = SmsParser.parseTransaction(
            "SBISMS",
            "SBI: Rs.45,000.00 credited to your account XX5678 on 01-Jul-26 towards salary. -State Bank of India",
        )
        assertNotNull(result)
        assertEquals(TransactionCategory.SALARY_CREDIT, result!!.category)
        assertEquals(45000.0, result.amount, 0.01)
        assertEquals("5678", result.accountLast4)
        assertTrue(result.confidenceScore >= 90)
    }

    /** PRD §5.1 example 3 — UPI Debit (ICICI) */
    @Test fun prd_example3_upiDebit_icici() {
        val result = SmsParser.parseTransaction(
            "ICICIB",
            "ICICI Bank: Rs.500.00 debited from a/c XX1234 via UPI on 01/07/2026 to merchant@paytm. UPI Ref:123456789012.",
        )
        assertNotNull(result)
        assertEquals(TransactionCategory.UPI_DEBIT, result!!.category)
        assertEquals(500.0, result.amount, 0.01)
        assertEquals("merchant@paytm", result.vpa)
        assertEquals("merchant", result.merchantName)
        assertTrue(result.confidenceScore >= 90)
    }

    /** PRD §5.1 example 4 — UPI Credit (Axis) */
    @Test fun prd_example4_upiCredit_axis() {
        val result = SmsParser.parseTransaction(
            "AXISBK",
            "Axis Bank: Rs.1,000.00 credited to your a/c XX4321 via UPI from john@oksbi on 01-Jul-26.",
        )
        assertNotNull(result)
        assertEquals(TransactionCategory.UPI_CREDIT, result!!.category)
        assertEquals(1000.0, result.amount, 0.01)
        assertEquals("john@oksbi", result.vpa)
        assertEquals("john", result.merchantName)
        assertTrue(result.confidenceScore >= 90)
    }

    /** PRD §5.1 example 5 — NEFT Transfer (HDFC) → BANK_TRANSFER_DEBIT (debit direction) */
    @Test fun prd_example5_neft_hdfc() {
        val result = SmsParser.parseTransaction(
            "HDFCBK",
            "HDFC Bank: Rs.25,000.00 transferred via NEFT from your a/c XX1234 on 01-Jul-2026.",
        )
        assertNotNull(result)
        assertEquals(TransactionCategory.BANK_TRANSFER_DEBIT, result!!.category)
        assertEquals(25000.0, result.amount, 0.01)
        assertTrue(result.confidenceScore >= 85)
    }

    /** PRD §5.1 example 6 — ATM Withdrawal (SBI) */
    @Test fun prd_example6_atmWithdrawal_sbi() {
        val result = SmsParser.parseTransaction(
            "SBISMS",
            "SBI: Rs.2,000.00 withdrawn at ATM (Txn:12345678) from a/c XX1234 on 01-Jul-26.",
        )
        assertNotNull(result)
        assertEquals(TransactionCategory.ATM_WITHDRAWAL, result!!.category)
        assertEquals(2000.0, result.amount, 0.01)
        assertEquals("1234", result.accountLast4)
        assertTrue(result.confidenceScore >= 90)
    }

    /** PRD §5.1 example 7 — Credit Card Bill (ICICI) */
    @Test fun prd_example7_creditCardBill_icici() {
        val result = SmsParser.parseTransaction(
            "ICICIB",
            "ICICI Bank: Your Credit Card Bill of Rs.12,500.00 is due on 15-Jul-26. Min due: Rs.500. Pay to avoid late charges.",
        )
        assertNotNull(result)
        assertEquals(TransactionCategory.CREDIT_CARD_BILL, result!!.category)
        assertEquals(12500.0, result.amount, 0.01)
        assertNotNull(result.date)
        assertTrue(result.confidenceScore >= 85)
    }

    /** PRD §5.1 example 8 — OTP must return null (AC #2) */
    @Test fun prd_example8_otp_returnsNull() {
        val result = SmsParser.parseTransaction(
            "VM-SBIINB",
            "SBI: Your OTP for transaction is 456789. Valid for 10 mins. Do not share with anyone.",
        )
        assertNull("OTP SMS must return null — no transaction record created", result)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // AC #2 — OTP / promo filtering
    // ─────────────────────────────────────────────────────────────────────────

    @Test fun otp_hdfc_returnsNull() {
        val result = SmsParser.parseTransaction(
            "HDFCBK",
            "HDFC Bank: OTP for your transaction is 382910. Valid for 5 mins.",
        )
        assertNull(result)
    }

    @Test fun promo_noFinancialContent_returnsNull() {
        val result = SmsParser.parseTransaction(
            "VM-OFFERS",
            "Congratulations! You have won an exclusive voucher. Click here to claim your prize.",
        )
        assertNull(result)
    }

    @Test fun promo_withFinancialContent_notNull() {
        // Promo signals alone don't suppress a message that also has financial content
        val result = SmsParser.parseTransaction(
            "HDFCBK",
            "HDFC Bank: Cashback of Rs.50.00 credited to your a/c XX1234 on 01-Jul-26.",
        )
        assertNotNull("promo SMS with actual credit should still be parsed", result)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // AC #3 — Confidence ≤ 50 → ParsedTransaction returned, isImportEligible false
    // ─────────────────────────────────────────────────────────────────────────

    @Test fun lowConfidence_knownSenderNoAmountNoDate_notImportEligible() {
        val result = SmsParser.parseTransaction(
            "HDFCBK",
            "HDFC Bank: Dear Customer, please visit your nearest branch for further assistance.",
        )
        assertNotNull("low-confidence message should still be returned for analytics", result)
        assertTrue("should not be import-eligible", result!!.confidenceScore <= 50)
        assertTrue("isImportEligible should be false", !result.isImportEligible)
    }

    @Test fun highConfidence_knownSenderAmountDate_isImportEligible() {
        val result = SmsParser.parseTransaction(
            "HDFCBK",
            "HDFC Bank: EMI of Rs.5,000 debited from your a/c XX1234 on 15-Jul-26.",
        )
        assertNotNull(result)
        assertTrue("confident result should be import-eligible", result!!.isImportEligible)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // AC #4 — Unknown sender ignored completely (null)
    // ─────────────────────────────────────────────────────────────────────────

    @Test fun unknownSender_noBodyBankMention_returnsNull() {
        val result = SmsParser.parseTransaction(
            "VM-OFFERS",
            "Your monthly dues are pending. Please settle Rs.5,000 before the deadline.",
        )
        assertNull("Unknown sender with no bank name in body must return null", result)
    }

    @Test fun unknownSender_bodyHasBankName_parsedViaBodyFallback() {
        // Sender not in whitelist but body mentions known bank — should still be parsed
        val result = SmsParser.parseTransaction(
            "AD-UNKNOWN",
            "HDFC Bank: Rs.3,000.00 debited from your a/c XX9876 on 01-Jul-26.",
        )
        assertNotNull("Body bank mention should let parsing proceed", result)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // AC #5 — Unit test per classification rule
    // ─────────────────────────────────────────────────────────────────────────

    @Test fun classification_emiDebit() {
        val result = SmsParser.parseTransaction(
            "KOTAKB",
            "Kotak Bank: EMI of Rs.6,200 debited from your account XX3456 on 01-Jul-26 for Personal Loan.",
        )
        assertEquals(TransactionCategory.EMI_DEBIT, result?.category)
    }

    @Test fun classification_salaryCredit() {
        val result = SmsParser.parseTransaction(
            "SBISMS",
            "SBI: Payroll credit of Rs.52,000 received in your account XX1111 on 01-Jul-26.",
        )
        assertEquals(TransactionCategory.SALARY_CREDIT, result?.category)
    }

    @Test fun classification_upiDebit_withVpa() {
        val result = SmsParser.parseTransaction(
            "AXISBK",
            "Axis Bank: Rs.250.00 debited via UPI to swiggy@hdfcbank on 15-Jul-26. A/c XX7890.",
        )
        assertEquals(TransactionCategory.UPI_DEBIT, result?.category)
    }

    @Test fun classification_upiCredit_withVpa() {
        val result = SmsParser.parseTransaction(
            "ICICIB",
            "ICICI Bank: Rs.500 credited to your a/c XX4567 via UPI from rahul@okaxis on 15-Jul-26.",
        )
        assertEquals(TransactionCategory.UPI_CREDIT, result?.category)
    }

    @Test fun classification_imps() {
        val result = SmsParser.parseTransaction(
            "HDFCBK",
            "HDFC Bank: IMPS transfer of Rs.10,000 to beneficiary account on 15-Jul-26 from a/c XX1234.",
        )
        assertEquals(TransactionCategory.BANK_TRANSFER_DEBIT, result?.category)
    }

    @Test fun classification_atmWithdrawal() {
        val result = SmsParser.parseTransaction(
            "SBISMS",
            "SBI: Cash withdrawal of Rs.5,000 at ATM on 15-Jul-26 from a/c XX5678.",
        )
        assertEquals(TransactionCategory.ATM_WITHDRAWAL, result?.category)
    }

    @Test fun classification_creditCardBill_minDue() {
        val result = SmsParser.parseTransaction(
            "ICICIB",
            "ICICI Bank: Your credit card bill of Rs.8,200 is due on 20-Jul-26. Total due: Rs.8,200. Min due: Rs.400.",
        )
        assertEquals(TransactionCategory.CREDIT_CARD_BILL, result?.category)
    }

    @Test fun classification_utilityBill_bbps() {
        val result = SmsParser.parseTransaction(
            "HDFCBK",
            "HDFC Bank: Rs.1,200.00 paid via BBPS for your electricity bill on 01-Jul-26.",
        )
        assertEquals(TransactionCategory.UTILITY_BILL, result?.category)
    }

    @Test fun classification_investment_sip() {
        val result = SmsParser.parseTransaction(
            "HDFCBK",
            "HDFC Bank: Your SIP of Rs.5,000 in HDFC Mutual Fund has been processed on 05-Jul-26.",
        )
        assertEquals(TransactionCategory.INVESTMENT, result?.category)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // AC #6 — VPA merchant name extraction
    // ─────────────────────────────────────────────────────────────────────────

    @Test fun vpa_merchantNameExtracted_standard() {
        val result = SmsParser.parseTransaction(
            "ICICIB",
            "ICICI Bank: Rs.350.00 debited via UPI to zomato@axisbank on 15-Jul-26. A/c XX1234.",
        )
        assertNotNull(result)
        assertEquals("zomato@axisbank", result!!.vpa)
        assertEquals("zomato", result.merchantName)
    }

    @Test fun vpa_phoneNumberVpa_extracted() {
        val result = SmsParser.parseTransaction(
            "AXISBK",
            "Axis Bank: Rs.200.00 debited via UPI to 9876543210@ybl on 01-Jul-26. A/c XX4321.",
        )
        assertNotNull(result)
        assertEquals("9876543210@ybl", result!!.vpa)
        assertEquals("9876543210", result.merchantName)
    }

    @Test fun vpa_emailInBody_notExtractedAsVpa() {
        // Email addresses like support@hdfc.com must not be mistaken for UPI VPAs
        val result = SmsParser.parseTransaction(
            "HDFCBK",
            "HDFC Bank: Rs.1,000 debited. For queries contact support@hdfcbank.com on 01-Jul-26.",
        )
        // Should not classify as UPI since VPA extraction should skip email addresses
        assertNotNull(result)
        assertNull("email address should not be extracted as VPA", result!!.vpa)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // AC #7 — Salary with irregular (compact) date format
    // ─────────────────────────────────────────────────────────────────────────

    @Test fun salary_compactDateFormat_01Jul26() {
        val result = SmsParser.parseTransaction(
            "SBISMS",
            "SBI: Salary of Rs.38,000 credited to your ac XX5678 on 01Jul26. -SBI Bank",
        )
        assertNotNull(result)
        assertEquals(TransactionCategory.SALARY_CREDIT, result!!.category)
        assertEquals(38000.0, result.amount, 0.01)
        assertEquals("01Jul26", result.date)
    }

    @Test fun salary_numericDateFormat_slashSeparated() {
        val result = SmsParser.parseTransaction(
            "SBISMS",
            "State Bank of India: Your salary of INR 60,000 has been credited to a/c XX9876 on 01/07/2026.",
        )
        assertNotNull(result)
        assertEquals(TransactionCategory.SALARY_CREDIT, result!!.category)
        assertEquals(60000.0, result.amount, 0.01)
        assertNotNull(result.date)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // AC #8 — Amount parsing: commas and decimals
    // ─────────────────────────────────────────────────────────────────────────

    @Test fun amount_withCommas_parsedCorrectly() {
        val result = SmsParser.parseTransaction(
            "HDFCBK",
            "HDFC Bank: EMI of Rs.8,750 debited from your a/c XX1234 on 15-Jul-26.",
        )
        assertEquals(8750.0, result?.amount ?: 0.0, 0.01)
    }

    @Test fun amount_withDecimals_parsedCorrectly() {
        val result = SmsParser.parseTransaction(
            "HDFCBK",
            "HDFC Bank: EMI of Rs.2,500.00 debited from your a/c XX1234 on 15-Jul-26.",
        )
        assertEquals(2500.0, result?.amount ?: 0.0, 0.01)
    }

    @Test fun amount_inrPrefix_parsedCorrectly() {
        val result = SmsParser.parseTransaction(
            "ICICIB",
            "ICICI Bank: INR 9,999.50 debited from your account XX4321 on 15-Jul-26 for EMI.",
        )
        assertEquals(9999.50, result?.amount ?: 0.0, 0.01)
    }

    @Test fun amount_rupeeSymbol_parsedCorrectly() {
        val result = SmsParser.parseTransaction(
            "AXISBK",
            "Axis Bank: EMI of ₹ 4,250.00 debited on 01-Jul-26 for Auto Loan a/c XX7654.",
        )
        assertEquals(4250.0, result?.amount ?: 0.0, 0.01)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Confidence score contract — PRD §5.4
    // ─────────────────────────────────────────────────────────────────────────

    @Test fun confidence_knownSenderAmountDate_atLeast90() {
        val result = SmsParser.parseTransaction(
            "HDFCBK",
            "HDFC Bank: EMI of Rs.6,000 debited from your a/c XX4567 on 10-Jul-26.",
        )
        assertNotNull(result)
        assertTrue("PRD §5.4: known sender+amount+date must yield ≥ 90", result!!.confidenceScore >= 90)
    }

    @Test fun confidence_below50_notImportEligible() {
        val result = SmsParser.parseTransaction(
            "HDFCBK",
            "HDFC Bank: Please update your KYC details at your nearest branch.",
        )
        assertNotNull(result)
        assertTrue("score should be ≤ 50", result!!.confidenceScore <= 50)
        assertTrue("should not be import-eligible", !result.isImportEligible)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Additional edge cases
    // ─────────────────────────────────────────────────────────────────────────

    @Test fun equatedMonthlyInstalment_classifiedAsEmiDebit() {
        val result = SmsParser.parseTransaction(
            "ICICIB",
            "ICICI Bank: Equated monthly instalment of Rs.3,600.00 deducted for Car Loan a/c XX9012 on 05-Jul-26.",
        )
        assertEquals(TransactionCategory.EMI_DEBIT, result?.category)
    }

    @Test fun kotak_senderIdInWhitelist() {
        val result = SmsParser.parseTransaction(
            "KOTAKB",
            "Kotak Bank: Rs.10,000 credited to your account XX1234 as salary on 01-Jul-26.",
        )
        assertNotNull(result)
        assertTrue("KOTAKB sender should be recognised", result!!.confidenceScore >= 35)
    }

    @Test fun dltPrefixedSenderId_recognised() {
        // DLT format: "AX-AXISBK" where "AX" is the telecom prefix
        val result = SmsParser.parseTransaction(
            "AX-AXISBK",
            "Axis Bank: EMI of Rs.7,500 debited from a/c XX3456 on 01-Jul-26.",
        )
        assertNotNull(result)
        assertTrue("DLT-prefixed sender should be recognised", result!!.confidenceScore >= 90)
    }
}
