package com.emireminder.app.sms

enum class TransactionCategory {
    EMI_DEBIT,
    SALARY_CREDIT,
    UPI_DEBIT,
    UPI_CREDIT,
    NEFT_IMPS,
    ATM_WITHDRAWAL,
    CREDIT_CARD_BILL,
    UTILITY_BILL,
    INVESTMENT,
    IGNORED,   // OTP / promotional — never stored as transactions
    UNKNOWN,
}
