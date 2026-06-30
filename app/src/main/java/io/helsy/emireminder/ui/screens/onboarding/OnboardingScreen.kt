package io.helsy.emireminder.ui.screens.onboarding

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
import io.helsy.emireminder.ui.theme.*

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
fun OnboardingScreen(onComplete: () -> Unit) {
    var page by remember { mutableIntStateOf(0) }
    val smsPermission = rememberPermissionState(android.Manifest.permission.READ_SMS)
    val notifPermission = if (android.os.Build.VERSION.SDK_INT >= 33) {
        rememberPermissionState(android.Manifest.permission.POST_NOTIFICATIONS)
    } else null

    val current = pages[page]

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Skip button
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            if (page < pages.lastIndex) {
                TextButton(onClick = onComplete) {
                    Text("Skip", color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center,
                )

                Spacer(Modifier.height(12.dp))

                Text(
                    pageData.body,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
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
            Button(
                onClick = {
                    smsPermission.launchPermissionRequest()
                    page++
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Indigo600),
            ) { Text(current.permissionLabel, fontSize = 16.sp, fontWeight = FontWeight.SemiBold) }
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = { page++ }) { Text("Skip for now", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        } else if (current.permissionLabel != null && page == 2) {
            Button(
                onClick = {
                    notifPermission?.launchPermissionRequest()
                    onComplete()
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Indigo600),
            ) { Text(current.permissionLabel, fontSize = 16.sp, fontWeight = FontWeight.SemiBold) }
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onComplete) { Text("Skip for now", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        } else {
            Button(
                onClick = { if (page < pages.lastIndex) page++ else onComplete() },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Indigo600),
            ) {
                Text(
                    if (page < pages.lastIndex) "Next" else "Get Started",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }

        Spacer(Modifier.height(16.dp))
    }
}
