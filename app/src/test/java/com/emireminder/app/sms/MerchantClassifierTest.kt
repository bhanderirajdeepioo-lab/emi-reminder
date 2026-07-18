package com.emireminder.app.sms

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MerchantClassifierTest {

    // AC-1: FOOD_AND_DINING via body
    @Test
    fun `AC1 body Swiggy returns FOOD_AND_DINING`() {
        assertEquals(
            TransactionCategory.FOOD_AND_DINING,
            MerchantClassifier.classify("Paid 350 to Swiggy"),
        )
    }

    // AC-1: FOOD_AND_DINING via VPA
    @Test
    fun `AC1 VPA swiggy returns FOOD_AND_DINING`() {
        assertEquals(
            TransactionCategory.FOOD_AND_DINING,
            MerchantClassifier.classify("UPI payment of 350", vpa = "swiggy@icici"),
        )
    }

    // AC-2: TRANSPORT via VPA
    @Test
    fun `AC2 VPA olacabs returns TRANSPORT`() {
        assertEquals(
            TransactionCategory.TRANSPORT,
            MerchantClassifier.classify("UPI payment of 150", vpa = "olacabs@olamoney"),
        )
    }

    // AC-2: TRANSPORT via body
    @Test
    fun `AC2 body Ola returns TRANSPORT`() {
        assertEquals(
            TransactionCategory.TRANSPORT,
            MerchantClassifier.classify("Paid 150 to Ola"),
        )
    }

    // AC-3: SHOPPING via VPA
    @Test
    fun `AC3 VPA amazon returns SHOPPING`() {
        assertEquals(
            TransactionCategory.SHOPPING,
            MerchantClassifier.classify("UPI payment of 999", vpa = "amazon@apl"),
        )
    }

    // AC-3: SHOPPING via body
    @Test
    fun `AC3 body Amazon returns SHOPPING`() {
        assertEquals(
            TransactionCategory.SHOPPING,
            MerchantClassifier.classify("Paid 999 to Amazon"),
        )
    }

    // AC-4: HEALTH via body
    @Test
    fun `AC4 body Apollo Pharmacy returns HEALTH`() {
        assertEquals(
            TransactionCategory.HEALTH,
            MerchantClassifier.classify("Paid 450 to Apollo Pharmacy"),
        )
    }

    // AC-5: BANK_CHARGES via body — service charge
    @Test
    fun `AC5 body service charge returns BANK_CHARGES`() {
        assertEquals(
            TransactionCategory.BANK_CHARGES,
            MerchantClassifier.classify("service charge of 100 debited from your account"),
        )
    }

    // AC-5: BANK_CHARGES via body — annual fee
    @Test
    fun `AC5 body annual fee returns BANK_CHARGES`() {
        assertEquals(
            TransactionCategory.BANK_CHARGES,
            MerchantClassifier.classify("annual fee of 500 charged to your credit card"),
        )
    }

    // AC-6: no match returns null
    @Test
    fun `AC6 unknown merchant returns null`() {
        assertNull(MerchantClassifier.classify("Payment of 100 to UnknownLocalShop"))
    }

    // AC-7: VPA match wins over body match
    @Test
    fun `AC7 VPA match wins over body match`() {
        // VPA says FOOD (swiggy), body says TRANSPORT (Ola)
        assertEquals(
            TransactionCategory.FOOD_AND_DINING,
            MerchantClassifier.classify(
                body = "Paid 150 to Ola",
                vpa = "swiggy@icici",
            ),
        )
    }

    // AC-8: case-insensitive body matching
    @Test
    fun `AC8 body SWIGGY uppercase returns FOOD_AND_DINING`() {
        assertEquals(
            TransactionCategory.FOOD_AND_DINING,
            MerchantClassifier.classify("SWIGGY payment"),
        )
    }

    @Test
    fun `AC8 body swiggy lowercase returns FOOD_AND_DINING`() {
        assertEquals(
            TransactionCategory.FOOD_AND_DINING,
            MerchantClassifier.classify("swiggy payment"),
        )
    }

    @Test
    fun `AC8 body Swiggy mixed-case returns FOOD_AND_DINING`() {
        assertEquals(
            TransactionCategory.FOOD_AND_DINING,
            MerchantClassifier.classify("Swiggy payment"),
        )
    }

    // AC-8: case-insensitive VPA matching
    @Test
    fun `AC8 VPA SWIGGY uppercase returns FOOD_AND_DINING`() {
        assertEquals(
            TransactionCategory.FOOD_AND_DINING,
            MerchantClassifier.classify("payment", vpa = "SWIGGY@ICICI"),
        )
    }

    @Test
    fun `AC8 VPA Swiggy mixed returns FOOD_AND_DINING`() {
        assertEquals(
            TransactionCategory.FOOD_AND_DINING,
            MerchantClassifier.classify("payment", vpa = "Swiggy@icici"),
        )
    }

    // merchantName path
    @Test
    fun `merchantName Zomato returns FOOD_AND_DINING`() {
        assertEquals(
            TransactionCategory.FOOD_AND_DINING,
            MerchantClassifier.classify("UPI payment", merchantName = "Zomato"),
        )
    }
}
