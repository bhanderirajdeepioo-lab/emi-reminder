package com.emireminder.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import dagger.hilt.android.AndroidEntryPoint
import com.emireminder.app.notification.NotificationScheduler
import com.emireminder.app.ui.navigation.AppNavGraph
import com.emireminder.app.ui.theme.EmiReminderTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
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
