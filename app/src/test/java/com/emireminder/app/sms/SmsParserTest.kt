package com.emireminder.app.sms

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SmsParserTest {

    // ──── HDFC ────────────────────────────────────────────────────────────────

    @Test fun hdfc_emiDebitWithAccount() {
        val result = SmsParser.parse(
            "HDFCBK",
            "HDFC Bank: EMI of Rs.3,500.00 debited from your account XX1234 on 15-Jun-26. Loan A/c: XXXXXX1234.",
        )
        assertEquals(BankSource.HDFC, result.bank)
        assertEquals(3500.0, result.emiAmount, 0.01)
        assertTrue(result.confidence >= 0.8f)
    }

    @Test fun hdfc_emiDebitNoAccount() {
        val result = SmsParser.parse(
            "HDFCBK",
            "Dear Customer, Your EMI of Rs.5000 has been debited from your HDFC Bank account ending 1234 for your Home Loan.",
        )
        assertEquals(BankSource.HDFC, result.bank)
        assertEquals(5000.0, result.emiAmount, 0.01)
        assertTrue(result.confidence >= 0.8f)
    }

    @Test fun hdfc_emiShortFormat() {
        val result = SmsParser.parse(
            "HDFCBN",
            "HDFC: EMI Rs.8750 debited on 01-Jun-26. Available bal: Rs.12,345.00 in a/c XX5678.",
        )
        assertEquals(BankSource.HDFC, result.bank)
        assertEquals(8750.0, result.emiAmount, 0.01)
        assertTrue(result.confidence >= 0.8f)
    }

    @Test fun hdfc_inrFormat() {
        val result = SmsParser.parse(
            "HDFCBK",
            "Acct XX1234 debited INR 12,500.00 on 15-Jun-26 for EMI (HDFC Home Loan). Info: HDFC Bank.",
        )
        assertEquals(BankSource.HDFC, result.bank)
        assertEquals(12500.0, result.emiAmount, 0.01)
        assertTrue(result.confidence >= 0.8f)
    }

    @Test fun hdfc_processedKeyword() {
        val result = SmsParser.parse(
            "HDFCBANKL",
            "HDFC Bank: EMI of Rs.22,000 processed for your Personal Loan a/c XXXX7890 on 30-Jun-26.",
        )
        assertEquals(BankSource.HDFC, result.bank)
        assertEquals(22000.0, result.emiAmount, 0.01)
        assertTrue(result.confidence >= 0.8f)
    }

    @Test fun hdfc_rupeeSymbol() {
        val result = SmsParser.parse(
            "HDFCBK",
            "HDFC Bank: EMI ₹ 4,250.00 debited on 01-Jun-26 for Auto Loan a/c XX4321.",
        )
        assertEquals(BankSource.HDFC, result.bank)
        assertEquals(4250.0, result.emiAmount, 0.01)
        assertTrue(result.confidence >= 0.8f)
    }

    // ──── SBI ─────────────────────────────────────────────────────────────────

    @Test fun sbi_emiDebitWithAccount() {
        val result = SmsParser.parse(
            "SBIINB",
            "SBI: EMI of Rs.4,200 for loan A/c XXXXXXXX1234 debited on 15Jun26. Avl Bal: Rs.8,120.45",
        )
        assertEquals(BankSource.SBI, result.bank)
        assertEquals(4200.0, result.emiAmount, 0.01)
        assertTrue(result.confidence >= 0.8f)
    }

    @Test fun sbi_loanInstalment() {
        val result = SmsParser.parse(
            "SBIMSG",
            "State Bank of India: Your loan installment of Rs 6,750.00 has been debited from your account.",
        )
        assertEquals(BankSource.SBI, result.bank)
        assertEquals(6750.0, result.emiAmount, 0.01)
        assertTrue(result.confidence >= 0.8f)
    }

    @Test fun sbi_cardAutoDebit() {
        val result = SmsParser.parse(
            "SBICRD",
            "SBICARD: Auto debit of Rs.3,500 processed for EMI on 15-06-26.",
        )
        assertEquals(BankSource.SBI, result.bank)
        assertEquals(3500.0, result.emiAmount, 0.01)
        assertTrue(result.confidence >= 0.8f)
    }

    @Test fun sbi_shortFormat() {
        val result = SmsParser.parse(
            "SBIMSG",
            "SBI Loan EMI Rs.15,000 debited. A/c XXXX1234. Bal Rs.25,000. -SBI",
        )
        assertEquals(BankSource.SBI, result.bank)
        assertEquals(15000.0, result.emiAmount, 0.01)
        assertTrue(result.confidence >= 0.8f)
    }

    @Test fun sbi_inrFormat() {
        val result = SmsParser.parse(
            "SBIINB",
            "SBI: INR 9,999.00 debited from your account XXXX5678 as loan instalment on 01-Jun-26.",
        )
        assertEquals(BankSource.SBI, result.bank)
        assertEquals(9999.0, result.emiAmount, 0.01)
        assertTrue(result.confidence >= 0.8f)
    }

    @Test fun sbi_sbiCreditCard() {
        val result = SmsParser.parse(
            "SBICRD",
            "SBI Credit Card: Auto debit of Rs.7,500 done for EMI conversion on 28-Jun-26. Avail limit: Rs.50,000.",
        )
        assertEquals(BankSource.SBI, result.bank)
        assertEquals(7500.0, result.emiAmount, 0.01)
        assertTrue(result.confidence >= 0.8f)
    }

    // ──── ICICI ───────────────────────────────────────────────────────────────

    @Test fun icici_autoDebitFormat() {
        val result = SmsParser.parse(
            "ICICIB",
            "ICICI Bank: Auto-debit of Rs.7,500.00 has been done for your EMI on 01-Jun-26.",
        )
        assertEquals(BankSource.ICICI, result.bank)
        assertEquals(7500.0, result.emiAmount, 0.01)
        assertTrue(result.confidence >= 0.8f)
    }

    @Test fun icici_emiDeducted() {
        val result = SmsParser.parse(
            "ICINB",
            "ICICIBANK: EMI of Rs.4,250 for loan a/c XXXX1234 deducted on 15/06/2026.",
        )
        assertEquals(BankSource.ICICI, result.bank)
        assertEquals(4250.0, result.emiAmount, 0.01)
        assertTrue(result.confidence >= 0.8f)
    }

    @Test fun icici_inrFormat() {
        val result = SmsParser.parse(
            "ICICIB",
            "Dear Customer, INR 9,000.00 debited from your ICICI Bank a/c on 15-Jun-26 for EMI.",
        )
        assertEquals(BankSource.ICICI, result.bank)
        assertEquals(9000.0, result.emiAmount, 0.01)
        assertTrue(result.confidence >= 0.8f)
    }

    @Test fun icici_lombard() {
        val result = SmsParser.parse(
            "ICICIS",
            "ICICI Lombard: EMI of Rs.2,100 auto-debited from your account on 01-Jun-26.",
        )
        assertEquals(BankSource.ICICI, result.bank)
        assertEquals(2100.0, result.emiAmount, 0.01)
        assertTrue(result.confidence >= 0.8f)
    }

    @Test fun icici_autoDebitedKeyword() {
        val result = SmsParser.parse(
            "ICICIB",
            "ICICI Bank - Your EMI amount of Rs.11,750 has been auto-debited on 15-06-26. A/c XX5678.",
        )
        assertEquals(BankSource.ICICI, result.bank)
        assertEquals(11750.0, result.emiAmount, 0.01)
        assertTrue(result.confidence >= 0.8f)
    }

    @Test fun icici_rupeeSymbol() {
        val result = SmsParser.parse(
            "ICICIB",
            "ICICI Bank: Equated monthly instalment of ₹3,600.00 deducted for Car Loan a/c XX9012 on 05-Jun-26.",
        )
        assertEquals(BankSource.ICICI, result.bank)
        assertEquals(3600.0, result.emiAmount, 0.01)
        assertTrue(result.confidence >= 0.8f)
    }

    // ──── Generic fallback ────────────────────────────────────────────────────

    @Test fun generic_emiKeywordWithAmount() {
        val result = SmsParser.parse(
            "VM-AXISBNK",
            "Your loan EMI of Rs.5,000 has been auto-debited successfully on 10-Jun-26.",
        )
        assertEquals(BankSource.GENERIC, result.bank)
        assertEquals(5000.0, result.emiAmount, 0.01)
        assertTrue(result.confidence >= 0.4f)
    }

    @Test fun unrelated_sms_returnsUnknownLowConfidence() {
        val result = SmsParser.parse(
            "VM-OFFERS",
            "You have won a prize! Click here to claim.",
        )
        assertEquals(BankSource.UNKNOWN, result.bank)
        assertTrue(result.confidence < 0.4f)
    }

    // ──── Confidence threshold contract ──────────────────────────────────────

    @Test fun confidenceContract_highConfidenceIsAutoImportEligible() {
        val result = SmsParser.parse(
            "HDFCBK",
            "HDFC Bank: EMI of Rs.6,000 debited from your account XX4567 on 10-Jun-26.",
        )
        assertTrue("Expected confidence >= 0.8 for auto-import", result.isConfident)
    }

    @Test fun confidenceContract_lowConfidenceIsNotAutoImportEligible() {
        val result = SmsParser.parse("VM-OFFERS", "Recharge now and get 10% cashback!")
        assertTrue("Expected low confidence for unrelated SMS", result.confidence < 0.4f)
    }
}
