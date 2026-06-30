package io.helsy.emireminder.ui.screens.onboarding

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun OnboardingScreen(onComplete: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Welcome to EMI Reminder", fontSize = 28.sp, fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(16.dp))
        Text("Track your EMIs, set reminders, and manage all your loans in one place.",
            style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onBackground)
        Spacer(Modifier.height(48.dp))
        Button(onClick = onComplete, modifier = Modifier.fillMaxWidth()) {
            Text("Get Started")
        }
    }
}
