package com.emireminder.app.ui.screens.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.emireminder.app.data.db.entity.Loan
import com.emireminder.app.domain.model.LoanType
import com.emireminder.app.domain.model.toLoanType
import com.emireminder.app.ui.theme.Indigo100
import com.emireminder.app.ui.theme.Indigo50
import com.emireminder.app.ui.theme.Indigo600
import com.emireminder.app.ui.theme.Slate800
import com.emireminder.app.ui.theme.UrgentRed
import com.emireminder.app.ui.theme.Violet600
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Locale

private val headerGradient = Brush.linearGradient(
    colors = listOf(Indigo600, Violet600)
)

@Composable
fun HomeScreen(
    onNavigateToLoanDetail: (Int) -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToAnalytics: () -> Unit,
    onNavigateToReminders: () -> Unit,
    onNavigateToSmsImport: () -> Unit,
    onNavigateToAddLoan: () -> Unit,
    onNavigateToAddReminder: () -> Unit,
    onNavigateToCalculator: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val loans by viewModel.activeLoans.collectAsState()
    val reminderCount by viewModel.activeReminderCount.collectAsState()

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToAddLoan,
                containerColor = Indigo600,
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Loan", tint = Color.White)
            }
        },
        containerColor = Indigo50,
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(bottom = 88.dp),
        ) {
            item {
                DashboardHeader(
                    onNavigateToSettings = onNavigateToSettings,
                    reminderCount = reminderCount,
                )
            }

            if (loans.isEmpty()) {
                item { EmptyState(onNavigateToAddLoan, onNavigateToSmsImport) }
            } else {
                item { LoanSummarySection(loans) }
                item { QuickActionsSection(onNavigateToAddLoan, onNavigateToAnalytics, onNavigateToCalculator) }
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "UPCOMING EMIs",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF64748B),
                            letterSpacing = 0.5.sp,
                        )
                        Text(
                            "See all →",
                            fontSize = 12.sp,
                            color = Indigo600,
                            modifier = Modifier.clickable { onNavigateToReminders() },
                        )
                    }
                }
                items(loans.take(5), key = { it.id }) { loan ->
                    LoanReminderCard(loan = loan, onClick = { onNavigateToLoanDetail(loan.id) })
                }
            }
        }
    }
}

// ── Header ────────────────────────────────────────────────────────────────────

@Composable
private fun DashboardHeader(onNavigateToSettings: () -> Unit, reminderCount: Int = 0) {
    val dateText = remember {
        LocalDate.now().format(DateTimeFormatter.ofPattern("EEE, d MMM yyyy", Locale.ENGLISH))
    }
    // user_name preference is not yet persisted anywhere; avoids a main-thread SharedPreferences
    // disk read on first composition. Re-introduce once a settings field writes this key.
    val userName = ""
    val greeting = remember {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        when {
            hour < 12 -> "Good morning"
            hour < 17 -> "Good afternoon"
            else      -> "Good evening"
        }
    }
    val initials = remember(userName) {
        if (userName.isBlank()) "₹"
        else userName.trim().split(" ").take(2).joinToString("") { it.first().uppercase() }
    }
    val displayGreeting = remember(userName) {
        if (userName.isBlank()) greeting else "$greeting, ${userName.trim().split(" ").first()}"
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(headerGradient)
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 20.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Avatar initials circle
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF312E81)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    initials,
                    fontSize = if (initials == "₹") 20.sp else 15.sp,
                    color = Indigo100,
                    fontWeight = FontWeight.Bold,
                )
            }

            Spacer(Modifier.width(12.dp))

            // Greeting + date
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    displayGreeting,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                )
                Text(dateText, fontSize = 12.sp, color = Color(0xFFC7D2FE))
            }

            // Settings gear
            IconButton(onClick = onNavigateToSettings) {
                Icon(
                    Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = Color(0xFFE0E7FF),
                    modifier = Modifier.size(22.dp),
                )
            }

            // Notification bell with live reminder badge
            Box {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF3730A3)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Default.Notifications,
                        contentDescription = "Notifications",
                        tint = Color(0xFFE0E7FF),
                        modifier = Modifier.size(20.dp),
                    )
                }
                if (reminderCount > 0) {
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .clip(CircleShape)
                            .background(UrgentRed)
                            .align(Alignment.TopEnd),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            if (reminderCount > 9) "9+" else reminderCount.toString(),
                            fontSize = 8.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
    }
}

// ── Empty state ───────────────────────────────────────────────────────────────

@Composable
private fun EmptyState(
    onAddLoan: () -> Unit,
    onSmsImport: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Illustration
        Box(
            modifier = Modifier
                .size(200.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(Indigo100, Color(0xFFEDE9FE)))),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(136.dp)
                    .clip(CircleShape)
                    .background(Color.White),
                contentAlignment = Alignment.Center,
            ) {
                Text("₹", fontSize = 56.sp, color = Indigo600, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(Modifier.height(24.dp))
        Text("No Loans Yet!", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = Slate800)
        Spacer(Modifier.height(8.dp))
        Text(
            "Add your first loan to start tracking\nEMIs and get smart reminders.",
            fontSize = 14.sp,
            color = Color(0xFF64748B),
            lineHeight = 22.sp,
        )

        Spacer(Modifier.height(32.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Calculate EMI card
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Indigo50),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Default.Calculate, contentDescription = null, tint = Indigo600)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("Calculate EMI", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Slate800)
                    Text("Try the calculator", fontSize = 11.sp, color = Color(0xFF64748B))
                }
            }

            // Add Loan card
            Card(
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onAddLoan),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Indigo600),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF312E81)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = Color.White)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("Add Loan", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
                    Text("Start tracking", fontSize = 11.sp, color = Color(0xFFA5B4FC))
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // SMS import banner
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onSmsImport),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("💬", fontSize = 20.sp)
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Detect from SMS", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Slate800)
                    Text("Auto-import EMIs from bank messages", fontSize = 12.sp, color = Color(0xFF64748B))
                }
                Text("›", fontSize = 22.sp, color = Indigo600)
            }
        }
    }
}

// ── Loan summary section ──────────────────────────────────────────────────────

@Composable
private fun LoanSummarySection(loans: List<Loan>) {
    val totalEmi = remember(loans) { loans.sumOf { it.emiAmount } }
    val currencyFmt = remember { NumberFormat.getCurrencyInstance(Locale("en", "IN")) }

    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Spacer(Modifier.height(16.dp))
        SectionLabel("LOAN SUMMARY")
        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Total active loans
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Indigo600),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Total Active Loans", fontSize = 11.sp, color = Color(0xFFC7D2FE))
                    Spacer(Modifier.height(8.dp))
                    Text("${loans.size}", fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                    val loanTypesSummary = remember(loans) {
                        loans.take(3).joinToString(" · ") { it.loanType.displayName }
                    }
                    Text(
                        loanTypesSummary,
                        fontSize = 11.sp,
                        color = Color(0xFFA5B4FC),
                    )
                }
            }

            // Next due
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0891B2)),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Next Due Date", fontSize = 11.sp, color = Color(0xFFBAE6FD))
                    Spacer(Modifier.height(8.dp))
                    Text("3", fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                    Text("days remaining", fontSize = 11.sp, color = Color(0xFFE0F2FE))
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // Total EMI / month with mini trend chart
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Slate800),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Total EMI / Month", fontSize = 11.sp, color = Color(0xFF94A3B8))
                Spacer(Modifier.height(8.dp))
                Text(
                    currencyFmt.format(totalEmi),
                    fontSize = 26.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                )
                Spacer(Modifier.height(12.dp))
                // Mini horizontal bar chart: placeholder trend for last 6 months
                MiniBarChart(
                    bars = listOf(0.60f, 0.72f, 0.65f, 0.80f, 0.75f, 1.00f),
                    barColor = Indigo600.copy(alpha = 0.7f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp),
                )
            }
        }
    }
}

// ── Quick actions ─────────────────────────────────────────────────────────────

@Composable
private fun QuickActionsSection(onAddLoan: () -> Unit, onAnalytics: () -> Unit, onCalculator: () -> Unit) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Spacer(Modifier.height(16.dp))
        SectionLabel("QUICK ACTIONS")
        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            QuickActionTile(
                icon = Icons.Default.Calculate,
                label = "Calculator",
                bgColor = Indigo50,
                iconColor = Indigo600,
                modifier = Modifier.weight(1f),
                onClick = onCalculator,
            )
            QuickActionTile(
                icon = Icons.Default.Add,
                label = "Add Loan",
                bgColor = Color(0xFFF0FDF4),
                iconColor = Color(0xFF059669),
                modifier = Modifier.weight(1f),
                onClick = onAddLoan,
            )
            QuickActionTile(
                icon = Icons.Default.Analytics,
                label = "Analytics",
                bgColor = Color(0xFFFFF7ED),
                iconColor = Color(0xFFD97706),
                modifier = Modifier.weight(1f),
                onClick = onAnalytics,
            )
        }
    }
}

@Composable
private fun QuickActionTile(
    icon: ImageVector,
    label: String,
    bgColor: Color,
    iconColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(bgColor),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = label, tint = iconColor, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.height(6.dp))
            Text(label, fontSize = 11.sp, color = Slate800, fontWeight = FontWeight.Medium)
        }
    }
}

// ── EMI reminder card ─────────────────────────────────────────────────────────

@Composable
private fun LoanReminderCard(loan: Loan, onClick: () -> Unit) {
    val loanAmountFmt = remember { NumberFormat.getNumberInstance(Locale("en", "IN")) }
    val urgentColor = UrgentRed
    val normalColor = Indigo600
    val isUrgent = false // placeholder — connect to real due-date logic when available

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(
            modifier = Modifier.height(IntrinsicSize.Min),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Urgency stripe
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .fillMaxHeight()
                    .background(if (isUrgent) urgentColor else normalColor),
            )

            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Loan type icon area
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Indigo50),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(loanTypeEmoji(loan.type), fontSize = 20.sp)
                }

                Spacer(Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        loan.name,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Slate800,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        loan.bankName.ifBlank { loan.loanType.displayName },
                        fontSize = 12.sp,
                        color = Color(0xFF64748B),
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "₹${loanAmountFmt.format(loan.emiAmount.toLong())}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate800,
                    )
                    Spacer(Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(11.dp))
                            .background(Indigo50)
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                    ) {
                        Text("Due in 12d", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Indigo600)
                    }
                }
            }
        }
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

@Composable
private fun MiniBarChart(
    bars: List<Float>,
    barColor: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val barCount = bars.size
        val gap = size.width * 0.04f
        val totalGap = gap * (barCount - 1)
        val barWidth = (size.width - totalGap) / barCount
        bars.forEachIndexed { index, fraction ->
            val barHeight = size.height * fraction.coerceIn(0.1f, 1.0f)
            val left = index * (barWidth + gap)
            val top = size.height - barHeight
            drawRoundRect(
                color = barColor,
                topLeft = Offset(left, top),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(4.dp.toPx()),
            )
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        color = Color(0xFF64748B),
        letterSpacing = 0.5.sp,
    )
}

private fun loanTypeEmoji(type: String): String = when (type.toLoanType()) {
    LoanType.HOME      -> "🏠"
    LoanType.CAR       -> "🚗"
    LoanType.PERSONAL  -> "👤"
    LoanType.EDUCATION -> "🎓"
    LoanType.BUSINESS  -> "💼"
    LoanType.OTHER     -> "💰"
}
