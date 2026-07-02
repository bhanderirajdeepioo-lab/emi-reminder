package com.emireminder.app.receiver

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Documents the expected UPI URI format produced by ACTION_PAY_NOW.
 *
 * Production code uses android.net.Uri.encode() (not available in JVM unit tests).
 * Tests here use the same percent-encoding rules via pure Kotlin to verify the contract
 * that special characters in a VPA must not corrupt the query string.
 *
 * Encoding table (unreserved chars per RFC 3986 + '@' safe in query values):
 *   safe: A-Z a-z 0-9 - _ . ~ @
 *   encoded: space→%20, &→%26, =→%3D, #→%23, +→%2B, etc.
 */
class PayNowUriTest {

    /** Mirrors the production Uri.encode() semantics for pure-JVM testing. */
    private fun encodeVpa(vpa: String): String = vpa.map { c ->
        if (c.isLetterOrDigit() || c in "-._~@") c.toString()
        else "%${c.code.toString(16).uppercase()}"
    }.joinToString("")

    private fun buildUri(vpa: String, amount: Double) =
        "upi://pay?pa=${encodeVpa(vpa)}&am=%.2f&cu=INR".format(amount)

    @Test
    fun `plain vpa without special chars is passed through unchanged`() {
        assertEquals(
            "upi://pay?pa=user@sbi&am=5000.00&cu=INR",
            buildUri("user@sbi", 5000.0),
        )
    }

    @Test
    fun `space in vpa is percent-encoded to %20`() {
        val uri = buildUri("user name@bank", 1000.0)
        assertFalse("raw space must not appear in URI", uri.contains(" "))
        assertTrue("space must be %20", uri.contains("pa=user%20name@bank"))
    }

    @Test
    fun `ampersand in vpa is encoded to prevent query-string injection`() {
        val uri = buildUri("evil&am=0@bank", 500.0)
        assertTrue("& must be encoded as %26", uri.contains("pa=evil%26am%3D0@bank"))
        // Without encoding, pa=evil would be truncated at the & and amount spoofed.
        assertFalse("raw & must not split pa parameter", uri.substringAfter("pa=").substringBefore("&am=").contains("&"))
    }

    @Test
    fun `amount is always formatted to two decimal places`() {
        assertTrue(buildUri("a@b", 9999.9).contains("am=9999.90"))
        assertTrue(buildUri("a@b", 100.0).contains("am=100.00"))
    }

    @Test
    fun `currency code is always INR`() {
        assertTrue(buildUri("a@b", 1.0).endsWith("cu=INR"))
    }
}
