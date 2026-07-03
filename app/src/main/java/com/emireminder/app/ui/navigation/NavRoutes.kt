package com.emireminder.app.ui.navigation

import java.net.URLEncoder

object NavRoutes {
    // --- Startup ---
    const val SPLASH           = "splash"
    const val LANGUAGE_SELECT  = "language_select"
    const val ONBOARDING       = "onboarding"
    const val COUNTRY_SELECT   = "country_select"

    // --- Bottom nav roots ---
    const val HOME               = "home"
    const val REMINDERS          = "reminders"
    const val FINANCE_TOOLS_HUB  = "finance_tools_hub"   // reachable from Finance tab toolbar
    const val FINANCE            = "finance"              // Finance tab

    // --- Home sub-screens ---
    const val EMPTY_HOME         = "empty_home"
    const val ADD_LOAN           = "add_loan"
    const val LOAN_ANALYTICS     = "loan_analytics"
    const val LOAN_DETAIL        = "loan_detail/{loanId}"
    const val SMS_IMPORT                    = "sms_import"
    const val SMS_INTELLIGENCE_ONBOARDING   = "sms_intelligence_onboarding"
    const val SMS_HISTORICAL_SCAN           = "sms_historical_scan"
    const val SETTINGS           = "settings"
    const val NOTIFICATION       = "notification"

    // --- Calculator screens ---
    const val EMI_CALCULATOR              = "emi_calculator"
    const val CALCULATOR_RESULTS          = "calculator_results/{principal}/{rate}/{tenure}/{loanType}"
    const val COMPARISON_CALCULATOR       = "comparison_calculator"
    const val PREPAYMENT_CALCULATOR       = "prepayment_calculator"
    const val PREPAYMENT_CALCULATOR_LOAN  = "prepayment_calculator_loan/{loanId}"
    const val INTEREST_TYPE_SELECTOR      = "interest_type/{principal}/{rate}/{tenure}/{currentType}"
    const val AMORTIZATION_SCHEDULE       = "amortization/{principal}/{rate}/{tenure}"

    // --- Finance sub-screens ---
    const val LOAN_CATEGORIES         = "loan_categories"
    const val FD_RD_CALCULATOR        = "fd_rd_calculator"
    const val SIP_CALCULATOR          = "sip_calculator"
    // EMI Calculator launched from LoanCategoriesScreen with a pre-selected label.
    // Kept as a separate route so the standard "emi_calculator" bottom-tab route is unchanged.
    const val EMI_CALCULATOR_PREFILL  = "emi_calculator_prefill/{label}"

    // --- SMS Finance Dashboard (HEL-569) ---
    const val SMS_MONTHLY_REPORT      = "sms_monthly_report/{yearMonth}"

    // --- Finance Accounts (HEL-570) ---
    const val FINANCE_ACCOUNTS        = "finance_accounts"

    // --- DPDP Act Right to Access (HEL-602) ---
    const val MY_FINANCE_DATA         = "my_finance_data"

    // --- Transaction Detail (HEL-591) ---
    const val TRANSACTION_DETAIL = "transaction_detail/{transactionId}"
    fun transactionDetail(id: String) = "transaction_detail/$id"

    // --- New Finance Tools (HEL-538) ---
    const val PPF_CALCULATOR          = "ppf_calculator"
    const val GST_CALCULATOR          = "gst_calculator"
    const val INCOME_TAX_CALCULATOR   = "income_tax_calculator"
    const val INFLATION_CALCULATOR    = "inflation_calculator"
    const val HRA_CALCULATOR          = "hra_calculator"
    const val CIBIL_SCORE             = "cibil_score"

    // --- Helpers ---
    fun smsMonthlyReport(yearMonth: String) = "sms_monthly_report/$yearMonth"
    fun loanDetail(loanId: Int)                = "loan_detail/$loanId"
    fun prepaymentCalculatorForLoan(loanId: Int) = "prepayment_calculator_loan/$loanId"
    fun calculatorResults(p: Double, r: Double, t: Int, loanType: String = "HOME") = "calculator_results/$p/$r/$t/$loanType"
    fun interestTypeSelector(p: Double, r: Double, t: Int, type: String) =
        "interest_type/$p/$r/$t/$type"
    fun amortizationSchedule(p: Double, r: Double, t: Int) = "amortization/$p/$r/$t"
    fun emiCalculatorWithLabel(label: String) =
        "emi_calculator_prefill/${URLEncoder.encode(label, "UTF-8")}"
}

val bottomNavRoutes = setOf(
    NavRoutes.HOME,
    NavRoutes.EMI_CALCULATOR,
    NavRoutes.REMINDERS,
    NavRoutes.FINANCE,
)
