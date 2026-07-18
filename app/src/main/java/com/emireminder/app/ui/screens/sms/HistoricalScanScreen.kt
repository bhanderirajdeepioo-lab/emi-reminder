package com.emireminder.app.ui.screens.sms

import android.app.Activity
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.emireminder.app.ui.theme.Indigo600
import com.emireminder.app.ui.theme.Indigo100
import com.emireminder.app.ui.theme.Violet600
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

// Replace with production interstitial unit ID before release
private const val INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712"

@Composable
fun HistoricalScanScreen(
    onNavigateToFinanceDashboard: () -> Unit,
    viewModel: HistoricalScanViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = context as? Activity

    LaunchedEffect(uiState) {
        val state = uiState
        if (state is HistoricalScanUiState.Complete) {
            if (state.showInterstitialAd && activity != null) {
                viewModel.onAdShown()
                InterstitialAd.load(
                    context,
                    INTERSTITIAL_AD_UNIT_ID,
                    AdRequest.Builder().build(),
                    object : InterstitialAdLoadCallback() {
                        override fun onAdLoaded(ad: InterstitialAd) {
                            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                                override fun onAdDismissedFullScreenContent() {
                                    onNavigateToFinanceDashboard()
                                }
                                override fun onAdFailedToShowFullScreenContent(error: AdError) {
                                    onNavigateToFinanceDashboard()
                                }
                            }
                            ad.show(activity)
                        }

                        override fun onAdFailedToLoad(error: LoadAdError) {
                            onNavigateToFinanceDashboard()
                        }
                    }
                )
            } else {
                // Re-scan (no ad) — navigate immediately
                onNavigateToFinanceDashboard()
            }
        }
    }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.linearGradient(listOf(Indigo600, Violet600)))
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Setting up Finance Intelligence",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0.dp),
    ) { padding ->
        AnimatedContent(
            targetState = uiState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            transitionSpec = { fadeIn(tween(300)).togetherWith(fadeOut(tween(200))) },
            label = "historical_scan_state",
        ) { state ->
            when (state) {
                is HistoricalScanUiState.Loading -> LoadingContent()
                is HistoricalScanUiState.Scanning -> ScanningContent(state)
                is HistoricalScanUiState.Complete -> CompleteContent(state, onNavigateToFinanceDashboard)
                is HistoricalScanUiState.Error -> ErrorContent(state, onNavigateToFinanceDashboard)
            }
        }
    }
}

@Composable
private fun LoadingContent() {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator(color = Indigo600)
        Spacer(Modifier.height(16.dp))
        Text(
            "Preparing to scan…",
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ScanningContent(state: HistoricalScanUiState.Scanning) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(Indigo600.copy(alpha = 0.12f), Indigo100))),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Default.Message,
                contentDescription = null,
                tint = Indigo600,
                modifier = Modifier.size(40.dp),
            )
        }

        Spacer(Modifier.height(32.dp))

        Text(
            "Scanning 6 months of bank SMS…",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(8.dp))

        Text(
            "This happens only once. Please keep the app open.",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            lineHeight = 22.sp,
        )

        Spacer(Modifier.height(32.dp))

        LinearProgressIndicator(
            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
            color = Indigo600,
            trackColor = Indigo600.copy(alpha = 0.15f),
        )

        Spacer(Modifier.height(16.dp))

        if (state.processedCount > 0) {
            Text(
                buildString {
                    append("Checked ${state.processedCount} messages")
                    if (state.insertedCount > 0) append(" · found ${state.insertedCount} transactions")
                },
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun CompleteContent(
    state: HistoricalScanUiState.Complete,
    onContinue: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(Color(0xFFF0FDF4)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = null,
                tint = Color(0xFF16A34A),
                modifier = Modifier.size(48.dp),
            )
        }

        Spacer(Modifier.height(24.dp))

        Text(
            "Scan complete!",
            fontSize = 24.sp,
            fontWeight = FontWeight.ExtraBold,
        )

        Spacer(Modifier.height(8.dp))

        val message = if (state.insertedCount > 0) {
            "Found ${state.insertedCount} financial transaction${if (state.insertedCount == 1) "" else "s"} from the last 6 months."
        } else {
            "No bank transactions found in the last 6 months."
        }

        Text(
            message,
            fontSize = 15.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            lineHeight = 23.sp,
        )

        Spacer(Modifier.height(40.dp))

        if (!state.showInterstitialAd) {
            Button(
                onClick = onContinue,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Indigo600),
            ) {
                Text("View Finance Dashboard", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
        } else {
            CircularProgressIndicator(
                modifier = Modifier.size(36.dp),
                color = Indigo600,
            )
        }
    }
}

@Composable
private fun ErrorContent(state: HistoricalScanUiState.Error, onContinue: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            Icons.Default.Warning,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(56.dp),
        )

        Spacer(Modifier.height(16.dp))

        Text(
            "Scan could not complete",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(8.dp))

        Text(
            state.message,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(32.dp))

        Button(
            onClick = onContinue,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Indigo600),
        ) {
            Text("Continue to Finance Dashboard", fontSize = 15.sp)
        }
    }
}
