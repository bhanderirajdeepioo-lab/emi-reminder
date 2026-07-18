package com.emireminder.app.sms

enum class TransactionCategory {
    EMI_DEBIT,
    SALARY_CREDIT,
    UPI_DEBIT,
    UPI_CREDIT,
    ATM_WITHDRAWAL,
    CREDIT_CARD_BILL,
    UTILITY_BILL,
    INVESTMENT,
    INSURANCE_PREMIUM,
    LOAN_DISBURSAL,
    TELECOM_RECHARGE,
    SUBSCRIPTION,
    REFUND,
    BANK_TRANSFER_DEBIT,
    BANK_TRANSFER_CREDIT,
    BALANCE_ALERT,
    IGNORED,   // OTP / promotional — never stored as transactions
    FOOD_AND_DINING,
    TRANSPORT,
    SHOPPING,
    HEALTH,
    BANK_CHARGES,
    UNKNOWN,
}
