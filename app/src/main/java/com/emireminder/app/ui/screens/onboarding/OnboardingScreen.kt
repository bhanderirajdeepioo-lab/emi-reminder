package com.emireminder.app.ui.screens.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.permissions.*
import com.emireminder.app.ui.theme.*

private data class OnboardingPage(
    val icon: ImageVector,
    val iconBg: Color,
    val title: String,
    val body: String,
    val permissionLabel: String? = null,
)

private val pages = listOf(
    OnboardingPage(
        icon = Icons.Default.TrackChanges,
        iconBg = Indigo600,
        title = "Track Every EMI",
        body = "Add all your loans in one place and never miss an EMI payment again. Get smart reminders before every due date.",
    ),
    OnboardingPage(
        icon = Icons.Default.Message,
        iconBg = Color(0xFF0891B2),
        title = "Auto-detect from SMS",
        body = "EMI Reminder reads your bank SMS to automatically detect loans and populate your dashboard — no manual entry needed.",
        permissionLabel = "Allow SMS Access",
    ),
    OnboardingPage(
        icon = Icons.Default.Notifications,
        iconBg = Color(0xFF7C3AED),
        title = "Never Miss a Payment",
        body = "Get timely notification reminders 3 days, 1 day, and on the day of your EMI due date.",
        permissionLabel = "Enable Notifications",
    ),
)

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun OnboardingScreen(onComplete: () -> Unit, onSkip: () -> Unit) {
    // Use remember (not rememberSaveable) so a Bundle-restored page index from a previous
    // interrupted onboarding session cannot put a returning user on page 2 unexpectedly (HEL-217).
    var page by remember { mutableIntStateOf(0) }
    val smsPermission = rememberPermissionState(android.Manifest.permission.READ_SMS)
    val notifPermission = if (android.os.Build.VERSION.SDK_INT >= 33) {
        rememberPermissionState(android.Manifest.permission.POST_NOTIFICATIONS)
    } else null
    var smsToggleState by remember { mutableStateOf(false) }
    var smsPermissionRequested by remember { mutableStateOf(false) }
    var notifPermissionRequested by remember { mutableStateOf(false) }

    // Advance page only after SMS permission dialog resolves (granted or denied)
    LaunchedEffect(smsPermission.status) {
        if (smsPermissionRequested) {
            smsToggleState = smsPermission.status.isGranted
            page = (page + 1).coerceAtMost(pages.lastIndex)
            smsPermissionRequested = false
        }
    }

    // Complete onboarding after notification permission dialog resolves.
    // Previously onComplete() was called in the button's onClick BEFORE the dialog
    // resolved, causing page 3 to disappear the instant it appeared (HEL-196).
    LaunchedEffect(notifPermission?.status) {
        if (notifPermissionRequested) {
            notifPermissionRequested = false
            onComplete()
        }
    }

    val current = pages[page]

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Skip button
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            if (page < pages.lastIndex) {
                TextButton(onClick = onSkip) {
                    Text("Skip", color = Color(0xFFB0AEC0))
                }
            }
        }

        Spacer(Modifier.weight(1f))

        AnimatedContent(
            targetState = current,
            transitionSpec = {
                (fadeIn(tween(400)) + slideInHorizontally(tween(400)) { it / 4 })
                    .togetherWith(fadeOut(tween(200)) + slideOutHorizontally(tween(400)) { -it / 4 })
            },
            label = "onboarding_page",
        ) { pageData ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // Icon illustration
                Box(
                    modifier = Modifier
                        .size(160.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(pageData.iconBg.copy(alpha = 0.15f), Indigo100))),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                            .background(pageData.iconBg),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(pageData.icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(48.dp))
                    }
                }

                Spacer(Modifier.height(40.dp))

                Text(
                    pageData.title,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                )

                Spacer(Modifier.height(12.dp))

                Text(
                    pageData.body,
                    fontSize = 15.sp,
                    color = Color(0xFFB0AEC0),
                    textAlign = TextAlign.Center,
                    lineHeight = 23.sp,
                )
            }
        }

        Spacer(Modifier.weight(1f))

        // Page indicators
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            pages.indices.forEach { i ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(3.dp))
                        .background(if (i == page) Indigo600 else Indigo100)
                        .size(width = if (i == page) 24.dp else 8.dp, height = 6.dp),
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        // Permission or next button
        if (current.permissionLabel != null && page == 1) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF312E81).copy(alpha = 0.8f)),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("SMS Permission", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFFE0E7FF))
                        Text("Auto-detect EMI from bank SMS", fontSize = 12.sp, color = Color(0xFF818CF8))
                    }
                    Switch(
                        checked = smsToggleState,
                        onCheckedChange = { on ->
                            if (on && !smsPermissionRequested) {
                                smsPermissionRequested = true
                                smsPermission.launchPermissionRequest()
                            }
                        },
                        colors = SwitchDefaults.colors(
                            uncheckedThumbColor = Color.White,
                            uncheckedTrackColor = Color(0xFF1E293B),
                        ),
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = { page++ }) { Text("Skip for now", color = Color(0xFFB0AEC0)) }
        } else if (current.permissionLabel != null && page == 2) {
            Button(
                onClick = {
                    if (notifPermission != null) {
                        // Wait for the dialog to resolve; LaunchedEffect above calls onComplete().
                        notifPermissionRequested = true
                        notifPermission.launchPermissionRequest()
                    } else {
                        // Android < 13: no runtime permission needed, complete immediately.
                        onComplete()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Indigo600),
            ) { Text(current.permissionLabel, fontSize = 16.sp, fontWeight = FontWeight.SemiBold) }
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onSkip) { Text("Skip for now", color = Color(0xFFB0AEC0)) }
        } else {
            // Only page 0 reaches this branch — always move forward, never call onComplete()
            Button(
                onClick = { page = page + 1 },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Indigo600),
            ) {
                Text("Next", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        Spacer(Modifier.height(16.dp))
    }
}
