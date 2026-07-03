package com.emireminder.app.ui.screens.sms

import android.content.Intent
import android.net.Uri
import android.provider.Settings
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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.emireminder.app.ui.theme.*

private data class BenefitItem(
    val icon: ImageVector,
    val title: String,
    val subtitle: String,
)

private val benefits = listOf(
    BenefitItem(Icons.Default.AutoAwesome,    "Auto-detect EMIs",      "We read bank SMS to find your loan payments automatically"),
    BenefitItem(Icons.Default.TrendingUp,     "Track spending",        "Understand your monthly outflow across all loans at a glance"),
    BenefitItem(Icons.Default.Dashboard,      "Finance dashboard",     "One unified view for all your EMIs, due dates, and balances"),
)

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun SmsIntelligenceOnboardingScreen(
    onGranted: () -> Unit,
    onBack: () -> Unit,
    viewModel: SmsIntelligenceViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val smsPermissions = rememberMultiplePermissionsState(
        permissions = listOf(
            android.Manifest.permission.READ_SMS,
            android.Manifest.permission.RECEIVE_SMS,
        )
    ) { results ->
        val allGranted = results.values.all { it }
        if (allGranted) {
            viewModel.onPermissionGranted()
        } else {
            viewModel.onPermissionDenied()
        }
    }

    LaunchedEffect(uiState) {
        if (uiState is SmsIntelligenceUiState.Granted) {
            onGranted()
        }
    }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.linearGradient(listOf(Indigo600, Violet600)))
                    .statusBarsPadding()
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Text(
                    "SMS Intelligence",
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
            transitionSpec = {
                fadeIn(tween(300)).togetherWith(fadeOut(tween(200)))
            },
            label = "sms_intelligence_state",
        ) { state ->
            when (state) {
                is SmsIntelligenceUiState.Intro,
                is SmsIntelligenceUiState.Requesting -> {
                    IntroContent(
                        onEnable = { smsPermissions.launchMultiplePermissionRequest() },
                    )
                }
                is SmsIntelligenceUiState.Denied -> {
                    DeniedContent(
                        onOpenSettings = {
                            val intent = Intent(
                                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                Uri.fromParts("package", context.packageName, null),
                            )
                            context.startActivity(intent)
                        },
                        onDismiss = {
                            viewModel.dismissNudge()
                            onBack()
                        },
                    )
                }
                is SmsIntelligenceUiState.Granted -> {
                    // LaunchedEffect above navigates away; show nothing while animating out
                    Box(modifier = Modifier.fillMaxSize())
                }
            }
        }
    }
}

@Composable
private fun IntroContent(onEnable: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(24.dp))

        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(Indigo600.copy(alpha = 0.15f), Indigo100))),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(Indigo600),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Default.Message,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(40.dp),
                )
            }
        }

        Spacer(Modifier.height(32.dp))

        Text(
            "Let your bank SMS\ndo the work",
            fontSize = 26.sp,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center,
            lineHeight = 34.sp,
        )

        Spacer(Modifier.height(8.dp))

        Text(
            "Enable SMS Intelligence to automatically detect your EMIs, track spending, and keep your finance dashboard up to date.",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            lineHeight = 22.sp,
        )

        Spacer(Modifier.height(32.dp))

        benefits.forEach { benefit ->
            BenefitRow(benefit)
            Spacer(Modifier.height(16.dp))
        }

        Spacer(Modifier.weight(1f))

        Button(
            onClick = onEnable,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Indigo600),
        ) {
            Icon(Icons.Default.Lock, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Enable SMS Intelligence", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun BenefitRow(benefit: BenefitItem) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Indigo50),
                contentAlignment = Alignment.Center,
            ) {
                Icon(benefit.icon, contentDescription = null, tint = Indigo600, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(benefit.title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Text(
                    benefit.subtitle,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp,
                )
            }
        }
    }
}

@Composable
private fun DeniedContent(onOpenSettings: () -> Unit, onDismiss: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(Color(0xFFFEF2F2)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Default.LockOpen,
                contentDescription = null,
                tint = Color(0xFFEF4444),
                modifier = Modifier.size(44.dp),
            )
        }

        Spacer(Modifier.height(24.dp))

        Text(
            "Permission not granted",
            fontSize = 22.sp,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(12.dp))

        Text(
            "You can enable SMS access later via\nSettings → SMS Access",
            fontSize = 15.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            lineHeight = 23.sp,
        )

        Spacer(Modifier.height(32.dp))

        Button(
            onClick = onOpenSettings,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Indigo600),
        ) {
            Icon(Icons.Default.Settings, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Open App Settings", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }

        Spacer(Modifier.height(12.dp))

        TextButton(onClick = onDismiss) {
            Text("Maybe later", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun SmsRevocationBanner(onDismiss: () -> Unit, onOpenSettings: () -> Unit) {
    val context = LocalContext.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Default.Warning,
                contentDescription = null,
                tint = Color(0xFFEF4444),
                modifier = Modifier.size(22.dp),
            )
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "SMS access revoked",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = Color(0xFFB91C1C),
                )
                Text(
                    "Tap to re-enable SMS Intelligence",
                    fontSize = 12.sp,
                    color = Color(0xFFEF4444),
                )
            }
            TextButton(
                onClick = {
                    val intent = Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.fromParts("package", context.packageName, null),
                    )
                    context.startActivity(intent)
                    onOpenSettings()
                },
                contentPadding = PaddingValues(horizontal = 8.dp),
            ) {
                Text("Fix", fontWeight = FontWeight.Bold, color = Indigo600, fontSize = 13.sp)
            }
            IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Close, contentDescription = "Dismiss", modifier = Modifier.size(16.dp))
            }
        }
    }
}
