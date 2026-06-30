package com.emireminder.app.domain.model

enum class LoanType(val displayName: String) {
    HOME("Home Loan"),
    CAR("Car Loan"),
    PERSONAL("Personal Loan"),
    EDUCATION("Education Loan"),
    BUSINESS("Business Loan"),
    OTHER("Other Loan"),
}

fun String.toLoanType(): LoanType =
    LoanType.entries.firstOrNull { it.name == this.uppercase() }
        ?: LoanType.entries.firstOrNull { it.displayName.equals(this, ignoreCase = true) }
        ?: LoanType.OTHER
