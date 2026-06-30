package com.emireminder.app.ui.navigation

import android.content.Context
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.emireminder.app.ui.screens.calculator.*
import com.emireminder.app.ui.screens.finance.*
import com.emireminder.app.ui.screens.home.HomeScreen
import com.emireminder.app.ui.screens.loan.*
import com.emireminder.app.ui.screens.onboarding.OnboardingScreen
import com.emireminder.app.ui.screens.reminders.*
import com.emireminder.app.ui.screens.settings.SettingsScreen
import com.emireminder.app.ui.screens.sms.SMSImportScreen
import com.emireminder.app.ui.screens.splash.SplashScreen
import com.emireminder.app.ui.theme.Indigo600

private data class NavItem(
    val route: String,
    val label: String,
    val filledIcon: ImageVector,
    val outlinedIcon: ImageVector,
)

private val bottomNavItems = listOf(
    NavItem(NavRoutes.HOME,             "Home",       Icons.Filled.Home,               Icons.Outlined.Home),
    NavItem(NavRoutes.EMI_CALCULATOR,   "Calculator", Icons.Filled.Calculate,          Icons.Outlined.Calculate),
    NavItem(NavRoutes.REMINDERS,        "Reminders",  Icons.Filled.Notifications,      Icons.Outlined.Notifications),
    NavItem(NavRoutes.FINANCE,          "Finance",    Icons.Filled.AccountBalanceWallet, Icons.Outlined.AccountBalanceWallet),
)

private fun isFirstLaunch(context: Context): Boolean =
    context.getSharedPreferences("emi_prefs", Context.MODE_PRIVATE)
        .getBoolean("is_first_launch", true)

private fun markOnboardingDone(context: Context) {
    // commit() instead of apply() so the write is flushed synchronously before process death;
    // apply() is async — an ANR kill between completion and flush resets app to onboarding.
    @Suppress("ApplySharedPref")
    context.getSharedPreferences("emi_prefs", Context.MODE_PRIVATE)
        .edit().putBoolean("is_first_launch", false).commit()
}

@Composable
fun AppNavGraph() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val context = LocalContext.current
    val firstLaunch = remember { isFirstLaunch(context) }
    val showBottomBar = currentRoute in bottomNavRoutes

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                FloatingNavBar(
                    currentRoute = currentRoute,
                    onNavigate = { route ->
                        navController.navigate(route) {
                            popUpTo(NavRoutes.HOME) {
                                // Never save sub-screen state for REMINDERS; add_reminder must
                                // not be restored when the user taps the Reminders tab again.
                                saveState = route != NavRoutes.REMINDERS
                            }
                            launchSingleTop = true
                            restoreState = route != NavRoutes.REMINDERS
                        }
                    }
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = NavRoutes.SPLASH,
            // Always apply innerPadding — when there is no bottom bar, Scaffold returns zero bottom
            // inset, so this is a no-op. Conditionally reading showBottomBar here caused the Scaffold
            // ScaffoldLayoutWithMeasureFix snapshotFlow to iterate a null IdentityArraySet entry
            // on focus-loss events (ANR: Input dispatching timed out after 5024 ms).
            modifier = Modifier.padding(innerPadding),
            enterTransition = { slideInHorizontally(tween(280)) { it } },
            exitTransition = { slideOutHorizontally(tween(280)) { -it } },
            popEnterTransition = { slideInHorizontally(tween(280)) { -it } },
            popExitTransition = { slideOutHorizontally(tween(280)) { it } },
        ) {
            // 1 — Splash
            composable(NavRoutes.SPLASH) {
                SplashScreen(
                    onNavigateToOnboarding = { navController.navigate(NavRoutes.ONBOARDING) { popUpTo(NavRoutes.SPLASH) { inclusive = true } } },
                    onNavigateToHome = { navController.navigate(NavRoutes.HOME) { popUpTo(NavRoutes.SPLASH) { inclusive = true } } },
                    isFirstLaunch = firstLaunch
                )
            }

            // 2 — Onboarding
            composable(NavRoutes.ONBOARDING) {
                OnboardingScreen(
                    onComplete = {
                        markOnboardingDone(context)
                        navController.navigate(NavRoutes.HOME) { popUpTo(NavRoutes.ONBOARDING) { inclusive = true } }
                    }
                )
            }

            // 3 — Home Dashboard (bottom tab)
            composable(NavRoutes.HOME) {
                HomeScreen(
                    onNavigateToLoanDetail  = { id -> navController.navigate(NavRoutes.loanDetail(id)) },
                    onNavigateToSettings    = { navController.navigate(NavRoutes.SETTINGS) },
                    onNavigateToAnalytics   = { navController.navigate(NavRoutes.LOAN_ANALYTICS) },
                    onNavigateToReminders   = {
                        navController.navigate(NavRoutes.REMINDERS) {
                            popUpTo(NavRoutes.HOME) { saveState = false }
                            launchSingleTop = true
                            restoreState = false
                        }
                    },
                    onNavigateToSmsImport   = { navController.navigate(NavRoutes.SMS_IMPORT) },
                    onNavigateToAddLoan     = { navController.navigate(NavRoutes.ADD_LOAN) },
                    onNavigateToAddReminder = {
                        navController.navigate(NavRoutes.REMINDERS) {
                            popUpTo(NavRoutes.HOME) { saveState = false }
                            launchSingleTop = true
                            restoreState = false
                        }
                    },
                )
            }

            // 4 — Empty Home (alternate start state)
            composable(NavRoutes.EMPTY_HOME) {
                HomeScreen(
                    onNavigateToLoanDetail  = { id -> navController.navigate(NavRoutes.loanDetail(id)) },
                    onNavigateToSettings    = { navController.navigate(NavRoutes.SETTINGS) },
                    onNavigateToAnalytics   = { navController.navigate(NavRoutes.LOAN_ANALYTICS) },
                    onNavigateToReminders   = { navController.navigate(NavRoutes.REMINDERS) },
                    onNavigateToSmsImport   = { navController.navigate(NavRoutes.SMS_IMPORT) },
                    onNavigateToAddLoan     = { navController.navigate(NavRoutes.ADD_LOAN) },
                    onNavigateToAddReminder = { navController.navigate(NavRoutes.REMINDERS) },
                )
            }

            // 5 — Reminders List (bottom tab) — Add Reminder sheet is owned by RemindersScreen
            composable(NavRoutes.REMINDERS) {
                RemindersScreen(
                    onReminderClick = { loanId -> navController.navigate(NavRoutes.loanDetail(loanId)) },
                    onNavigateToNotificationPreview = { navController.navigate(NavRoutes.NOTIFICATION) },
                )
            }

            // 7 — Notification preview screen
            composable(NavRoutes.NOTIFICATION) {
                NotificationPreviewScreen(onBack = { navController.popBackStack() })
            }

            // 8 — Finance Tools Hub / Calculator tab
            composable(NavRoutes.FINANCE_TOOLS_HUB) {
                FinanceToolsHubScreen(
                    onNavigateToEmiCalculator    = { navController.navigate(NavRoutes.EMI_CALCULATOR) },
                    onNavigateToComparison       = { navController.navigate(NavRoutes.COMPARISON_CALCULATOR) },
                    onNavigateToPrepayment       = { navController.navigate(NavRoutes.PREPAYMENT_CALCULATOR) },
                    onNavigateToFdRd             = { navController.navigate(NavRoutes.FD_RD_CALCULATOR) },
                    onNavigateToSip              = { navController.navigate(NavRoutes.SIP_CALCULATOR) },
                    onNavigateToLoanCategories   = { navController.navigate(NavRoutes.LOAN_CATEGORIES) },
                )
            }

            // 9 — EMI Calculator (also the Calculator bottom-nav tab destination)
            composable(NavRoutes.EMI_CALCULATOR) {
                val prevRoute = navController.previousBackStackEntry?.destination?.route
                val isTabEntry = prevRoute == null || prevRoute in bottomNavRoutes
                EMICalculatorScreen(
                    showBackButton = !isTabEntry,
                    onBack = { navController.popBackStack() },
                    onShowResults = { p, r, t -> navController.navigate(NavRoutes.calculatorResults(p, r, t)) },
                    onInterestTypeSelector = { p, r, t, type ->
                        navController.navigate(NavRoutes.interestTypeSelector(p, r, t, type))
                    },
                )
            }

            // 10 — Calculator Results
            composable(
                NavRoutes.CALCULATOR_RESULTS,
                arguments = listOf(
                    navArgument("principal") { type = NavType.StringType },
                    navArgument("rate")      { type = NavType.StringType },
                    navArgument("tenure")    { type = NavType.StringType },
                )
            ) { back ->
                val p = back.arguments?.getString("principal")?.toDoubleOrNull() ?: 0.0
                val r = back.arguments?.getString("rate")?.toDoubleOrNull() ?: 0.0
                val t = back.arguments?.getString("tenure")?.toIntOrNull() ?: 0
                CalculatorResultsScreen(
                    principal = p, rate = r, tenureMonths = t,
                    onBack = { navController.popBackStack() },
                    onViewAmortization = { navController.navigate(NavRoutes.amortizationSchedule(p, r, t)) },
                )
            }

            // 11 — Amortization Schedule
            composable(
                NavRoutes.AMORTIZATION_SCHEDULE,
                arguments = listOf(
                    navArgument("principal") { type = NavType.StringType },
                    navArgument("rate")      { type = NavType.StringType },
                    navArgument("tenure")    { type = NavType.StringType },
                )
            ) { back ->
                AmortizationScheduleScreen(
                    principal    = back.arguments?.getString("principal")?.toDoubleOrNull() ?: 0.0,
                    rate         = back.arguments?.getString("rate")?.toDoubleOrNull() ?: 0.0,
                    tenureMonths = back.arguments?.getString("tenure")?.toIntOrNull() ?: 0,
                    onBack = { navController.popBackStack() },
                )
            }

            // 12 — Comparison Calculator
            composable(NavRoutes.COMPARISON_CALCULATOR) {
                ComparisonCalculatorScreen(onBack = { navController.popBackStack() })
            }

            // 13 — Prepayment Calculator
            composable(NavRoutes.PREPAYMENT_CALCULATOR) {
                PrepaymentCalculatorScreen(onBack = { navController.popBackStack() })
            }

            // 14 — Interest Type Selector
            composable(
                NavRoutes.INTEREST_TYPE_SELECTOR,
                arguments = listOf(
                    navArgument("principal")   { type = NavType.StringType },
                    navArgument("rate")        { type = NavType.StringType },
                    navArgument("tenure")      { type = NavType.StringType },
                    navArgument("currentType") { type = NavType.StringType },
                )
            ) { back ->
                InterestTypeSelectorScreen(
                    principal    = back.arguments?.getString("principal")?.toDoubleOrNull() ?: 0.0,
                    rate         = back.arguments?.getString("rate")?.toDoubleOrNull() ?: 0.0,
                    tenureMonths = back.arguments?.getString("tenure")?.toIntOrNull() ?: 0,
                    currentType  = back.arguments?.getString("currentType") ?: "REDUCING",
                    onBack = { navController.popBackStack() },
                )
            }

            // 15 — SMS Import
            composable(NavRoutes.SMS_IMPORT) {
                SMSImportScreen(onBack = { navController.popBackStack() })
            }

            // 16 — Settings
            composable(NavRoutes.SETTINGS) {
                SettingsScreen(onBack = { navController.popBackStack() })
            }

            // Add Loan form
            composable(NavRoutes.ADD_LOAN) {
                AddLoanScreen(onBack = { navController.popBackStack() })
            }

            // 17 — Loan Analytics
            composable(NavRoutes.LOAN_ANALYTICS) {
                LoanAnalyticsScreen(onBack = { navController.popBackStack() })
            }

            // 18 — Loan Detail
            composable(
                NavRoutes.LOAN_DETAIL,
                arguments = listOf(navArgument("loanId") { type = NavType.IntType })
            ) { back ->
                LoanDetailScreen(
                    loanId = back.arguments?.getInt("loanId") ?: -1,
                    onBack = { navController.popBackStack() },
                    onViewAmortization = { p, r, t ->
                        navController.navigate(NavRoutes.amortizationSchedule(p, r, t))
                    },
                )
            }

            // 19 — Loan Categories
            composable(NavRoutes.LOAN_CATEGORIES) {
                LoanCategoriesScreen(
                    onCategorySelected = { navController.navigate(NavRoutes.EMI_CALCULATOR) },
                    onBack = { navController.popBackStack() },
                )
            }

            // 20 — Finance Monthly EMI (bottom tab)
            composable(NavRoutes.FINANCE) {
                FinanceMonthlyEMIScreen(
                    onNavigateToLoanDetail     = { id -> navController.navigate(NavRoutes.loanDetail(id)) },
                    onNavigateToFinanceToolsHub = { navController.navigate(NavRoutes.FINANCE_TOOLS_HUB) },
                )
            }

            // 21 — FD/RD Calculator
            composable(NavRoutes.FD_RD_CALCULATOR) {
                FDRDCalculatorScreen(onBack = { navController.popBackStack() })
            }

            // 22 — SIP Calculator
            composable(NavRoutes.SIP_CALCULATOR) {
                SIPCalculatorScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}

@Composable
private fun FloatingNavBar(currentRoute: String?, onNavigate: (String) -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .navigationBarsPadding()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(elevation = 16.dp, shape = RoundedCornerShape(32.dp))
                .clip(RoundedCornerShape(32.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 8.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                bottomNavItems.forEach { item ->
                    val selected = currentRoute == item.route
                    NavItem(item = item, selected = selected, onClick = {
                        if (!selected) onNavigate(item.route)
                    })
                }
            }
        }
    }
}

@Composable
private fun NavItem(item: NavItem, selected: Boolean, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (selected) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(Indigo600)
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(item.filledIcon, contentDescription = item.label, tint = Color.White, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(item.label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                }
            }
        } else {
            Icon(item.outlinedIcon, contentDescription = item.label,
                tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp))
            Spacer(Modifier.height(2.dp))
            Text(item.label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
