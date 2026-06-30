package io.helsy.emireminder.sms

enum class BankSource { HDFC, SBI, ICICI, GENERIC, UNKNOWN }

data class SmsParseResult(
    val bank: BankSource,
    val emiAmount: Double,
    val loanAccount: String,
    val confidence: Float,
    val rawBody: String,
    val senderAddress: String = "",
) {
    val isConfident: Boolean get() = confidence >= 0.8f
    val isUncertain: Boolean get() = confidence in 0.4f..0.8f
}
