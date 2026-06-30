package com.emireminder.app.ui.navigation

object NavRoutes {
    // --- Startup ---
    const val SPLASH        = "splash"
    const val ONBOARDING    = "onboarding"

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
    const val SMS_IMPORT         = "sms_import"
    const val SETTINGS           = "settings"
    const val NOTIFICATION       = "notification"

    // --- Calculator screens ---
    const val EMI_CALCULATOR         = "emi_calculator"
    const val CALCULATOR_RESULTS     = "calculator_results/{principal}/{rate}/{tenure}"
    const val COMPARISON_CALCULATOR  = "comparison_calculator"
    const val PREPAYMENT_CALCULATOR  = "prepayment_calculator"
    const val INTEREST_TYPE_SELECTOR = "interest_type/{principal}/{rate}/{tenure}/{currentType}"
    const val AMORTIZATION_SCHEDULE  = "amortization/{principal}/{rate}/{tenure}"

    // --- Finance sub-screens ---
    const val LOAN_CATEGORIES    = "loan_categories"
    const val FD_RD_CALCULATOR   = "fd_rd_calculator"
    const val SIP_CALCULATOR     = "sip_calculator"

    // --- Helpers ---
    fun loanDetail(loanId: Int)         = "loan_detail/$loanId"
    fun calculatorResults(p: Double, r: Double, t: Int) = "calculator_results/$p/$r/$t"
    fun interestTypeSelector(p: Double, r: Double, t: Int, type: String) =
        "interest_type/$p/$r/$t/$type"
    fun amortizationSchedule(p: Double, r: Double, t: Int) = "amortization/$p/$r/$t"
}

val bottomNavRoutes = setOf(
    NavRoutes.HOME,
    NavRoutes.EMI_CALCULATOR,
    NavRoutes.REMINDERS,
    NavRoutes.FINANCE,
)
