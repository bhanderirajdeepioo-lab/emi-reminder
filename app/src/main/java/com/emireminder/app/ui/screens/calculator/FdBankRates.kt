package com.emireminder.app.ui.screens.calculator

data class FdBankRate(val bankName: String, val ratePercent: Double)

/** Hardcoded indicative FD rates (% p.a.) — update here whenever rates change. */
val FD_BANK_RATES: List<FdBankRate> = listOf(
    FdBankRate("SBI",            7.10),
    FdBankRate("HDFC Bank",      7.25),
    FdBankRate("ICICI Bank",     7.20),
    FdBankRate("Axis Bank",      7.20),
    FdBankRate("PNB",            6.75),
    FdBankRate("Bajaj Finance",  8.35),
)
