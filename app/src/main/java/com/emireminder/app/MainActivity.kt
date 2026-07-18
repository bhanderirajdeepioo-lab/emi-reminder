package com.emireminder.app

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.emireminder.app.ads.AppOpenAdManager
import com.emireminder.app.ads.ConsentManager
import dagger.hilt.android.AndroidEntryPoint
import com.emireminder.app.notification.NotificationScheduler
import com.emireminder.app.ui.navigation.AppNavGraph
import com.emireminder.app.ui.theme.EmiReminderTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Only show App Open ad for returning users (onboarding complete).
        // First-time users go through onboarding; showing an interstitial ad during
        // that flow is poor UX and violates AdMob policy on forced ad views.
        val isReturningUser = !getSharedPreferences("emi_prefs", Context.MODE_PRIVATE)
            .getBoolean("is_first_launch", true)

        ConsentManager.requestConsent(this) {
            if (isReturningUser) {
                AppOpenAdManager.showAdIfAvailable(this)
            }
        }

        val deepLinkLoanId = intent.getIntExtra(NotificationScheduler.EXTRA_LOAN_ID, -1)
        setContent {
            EmiReminderTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppNavGraph(deepLinkLoanId = deepLinkLoanId)
                }
            }
        }
    }
}
