package io.helsy.emireminder.ui.screens.splash

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import io.helsy.emireminder.ui.theme.Indigo600
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    isFirstLaunch: Boolean,
    onNavigateToOnboarding: () -> Unit,
    onNavigateToHome: () -> Unit,
) {
    LaunchedEffect(Unit) {
        delay(1500)
        if (isFirstLaunch) onNavigateToOnboarding() else onNavigateToHome()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Indigo600),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "EMI",
                color = Color.White,
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Reminder & Calculator",
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 18.sp,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}
