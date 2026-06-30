package io.helsy.emireminder.ui.screens.reminders

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.helsy.emireminder.ui.theme.Indigo600
import io.helsy.emireminder.ui.theme.Violet600

private val LockBgStart = Color(0xFF0F172A)
private val LockBgEnd   = Color(0xFF1E1B4B)
private val CardBg      = Color(0x1A1E293B)      // semi-transparent dark
private val CardSurface = Color(0xFF1E293B)
private val TextPrimary = Color(0xFFFFFFFF)
private val TextSecondary = Color(0xFF94A3B8)
private val TextAccent  = Color(0xFFA5B4FC)
private val ChipDark    = Color(0xFF374151)

@Composable
fun NotificationPreviewScreen(onBack: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize()) {
        // Lock screen background
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.linearGradient(listOf(LockBgStart, LockBgEnd)))
        )

        // Ambient blobs
        Box(
            modifier = Modifier
                .size(280.dp)
                .offset(x = 180.dp, y = 40.dp)
                .clip(RoundedCornerShape(140.dp))
                .background(Color(0xFF312E81).copy(alpha = 0.3f))
        )
        Box(
            modifier = Modifier
                .size(240.dp)
                .offset(x = (-60).dp, y = 500.dp)
                .clip(RoundedCornerShape(120.dp))
                .background(LockBgEnd.copy(alpha = 0.4f))
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // Top bar (transparent)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White.copy(alpha = 0.7f))
                }
                Text(
                    "Notification Preview",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White.copy(alpha = 0.9f),
                    modifier = Modifier.padding(start = 4.dp),
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(Modifier.height(16.dp))

                // Lock icon + time
                Text("🔒", fontSize = 24.sp)
                Spacer(Modifier.height(12.dp))
                Text("9:41", fontSize = 54.sp, fontWeight = FontWeight.ExtraBold, color = TextPrimary)
                Text("Monday, 30 June 2026", fontSize = 16.sp, color = TextSecondary)

                Spacer(Modifier.height(32.dp))

                // Overdue notification card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(CardSurface.copy(alpha = 0.9f))
                        .padding(16.dp),
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // App icon
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Brush.linearGradient(listOf(Indigo600, Violet600))),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text("₹", fontSize = 18.sp, color = Color.White, fontWeight = FontWeight.Bold)
                            }
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("EMI Reminder", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            }
                            Text("now", fontSize = 11.sp, color = TextSecondary)
                        }
                        Spacer(Modifier.height(10.dp))
                        Text(
                            "⚠ EMI Due Today!",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "SBI Home Loan: ₹32,450 due on 30 Jun",
                            fontSize = 13.sp,
                            color = TextSecondary,
                        )
                        Spacer(Modifier.height(12.dp))
                        // Action chips
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            NotifChip("Pay Now", Indigo600)
                            NotifChip("Snooze 1h", ChipDark)
                            NotifChip("Dismiss", ChipDark)
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                // Upcoming notification card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(CardSurface.copy(alpha = 0.85f))
                        .padding(16.dp),
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFF0891B2)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text("₹", fontSize = 18.sp, color = Color.White, fontWeight = FontWeight.Bold)
                            }
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("EMI Reminder", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            }
                            Text("2 min ago", fontSize = 11.sp, color = TextSecondary)
                        }
                        Spacer(Modifier.height(10.dp))
                        Text(
                            "HDFC Car Loan — 5 days left",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "₹10,941 due on 5 Jul 2026",
                            fontSize = 13.sp,
                            color = TextSecondary,
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))

                // Expanded rich notification — EMI summary
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(CardSurface.copy(alpha = 0.80f))
                        .padding(16.dp),
                ) {
                    Column {
                        Text(
                            "This month's EMI Summary",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            modifier = Modifier.align(Alignment.CenterHorizontally),
                        )
                        Spacer(Modifier.height(12.dp))
                        EmiProgressRow(label = "Home Loan", amount = "₹32,450", fraction = 0.42f, barColor = Indigo600)
                        Spacer(Modifier.height(8.dp))
                        EmiProgressRow(label = "Car Loan", amount = "₹10,941", fraction = 0.67f, barColor = Color(0xFF0891B2))
                        Spacer(Modifier.height(12.dp))
                        Row {
                            Text("Total this month: ", fontSize = 11.sp, color = TextSecondary)
                            Text("₹43,391", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        }
                    }
                }

                Spacer(Modifier.height(48.dp))

                Text("swipe up to unlock", fontSize = 13.sp, color = Color(0xFF475569))
                Spacer(Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .size(width = 48.dp, height = 4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color(0xFF475569))
                )

                Spacer(Modifier.height(24.dp))

                // Caption for the user
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "About EMI Reminder Notifications",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextAccent,
                        )
                        Spacer(Modifier.height(8.dp))
                        NotifInfoRow("3 days before your due date")
                        NotifInfoRow("1 day before your due date")
                        NotifInfoRow("On your due date")
                        NotifInfoRow("Summary of all upcoming EMIs")
                    }
                }

                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun NotifChip(text: String, bg: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(13.dp))
            .background(bg)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Text(text, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
    }
}

@Composable
private fun EmiProgressRow(label: String, amount: String, fraction: Float, barColor: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, fontSize = 11.sp, color = TextSecondary, modifier = Modifier.width(80.dp))
        Box(
            modifier = Modifier
                .weight(1f)
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color(0xFF374151))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fraction)
                    .clip(RoundedCornerShape(4.dp))
                    .background(barColor)
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(amount, fontSize = 11.sp, color = TextPrimary, modifier = Modifier.width(68.dp))
    }
}

@Composable
private fun NotifInfoRow(text: String) {
    Row(modifier = Modifier.padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(Indigo600)
        )
        Spacer(Modifier.width(8.dp))
        Text(text, fontSize = 13.sp, color = Color(0xFF94A3B8))
    }
}
