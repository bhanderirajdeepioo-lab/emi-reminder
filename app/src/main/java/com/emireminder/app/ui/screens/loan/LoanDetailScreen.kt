package com.emireminder.app.ui.screens.loan

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.emireminder.app.data.db.entity.Loan
import com.emireminder.app.domain.model.LoanType
import com.emireminder.app.domain.model.toLoanType
import com.emireminder.app.ui.theme.*
import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.pow
import kotlin.math.roundToInt

private data class EmiHistoryEntry(val label: String, val isLate: Boolean, val amount: Double)

@Composable
fun LoanDetailScreen(
    loanId: Int,
    onBack: () -> Unit,
    onViewAmortization: (Double, Double, Int) -> Unit,
    onPrepay: () -> Unit = {},
    viewModel: LoanDetailViewModel = hiltViewModel(),
) {
    val loan by viewModel.loan.collectAsState()
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    val fmt = remember { NumberFormat.getCurrencyInstance(Locale("en", "IN")) }

    LaunchedEffect(loanId) { viewModel.loadLoan(loanId) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Loan?") },
            text = { Text("This will permanently delete '${loan?.name}' and all associated reminders.") },
            confirmButton = {
                TextButton(
                    onClick = { showDeleteDialog = false; viewModel.deleteLoan(onBack) },
                    colors = ButtonDefaults.textButtonColors(contentColor = UrgentRed),
                ) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") } },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(loan?.name ?: "Loan Detail", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } },
                actions = {
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Options")
                        }
                        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                            DropdownMenuItem(
                                text = { Text("Delete Loan", color = UrgentRed) },
                                leadingIcon = { Icon(Icons.Default.Delete, null, tint = UrgentRed) },
                                onClick = { showMenu = false; showDeleteDialog = true },
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Indigo600,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        val currentLoan = loan
        if (currentLoan == null) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Indigo600)
            }
        } else {
            LoanDetailContent(
                loan = currentLoan,
                fmt = fmt,
                padding = padding,
                onViewAmortization = onViewAmortization,
                onPrepay = onPrepay,
            )
        }
    }
}

@Composable
private fun LoanDetailContent(
    loan: Loan,
    fmt: NumberFormat,
    padding: PaddingValues,
    onViewAmortization: (Double, Double, Int) -> Unit,
    onPrepay: () -> Unit,
) {
    val monthsElapsed = remember(loan.startDate, loan.tenureMonths) {
        val startDate = Instant.ofEpochMilli(loan.startDate).atZone(ZoneId.systemDefault()).toLocalDate()
        ChronoUnit.MONTHS.between(startDate, LocalDate.now()).toInt().coerceIn(0, loan.tenureMonths)
    }
    val outstandingBalance = remember(loan.principalAmount, loan.interestRate, loan.tenureMonths, monthsElapsed) {
        computeOutstandingBalance(loan.principalAmount, loan.interestRate, loan.tenureMonths, monthsElapsed)
    }
    val tenureRemainingMonths = loan.tenureMonths - monthsElapsed
    val progressFraction = if (loan.tenureMonths > 0) (monthsElapsed.toFloat() / loan.tenureMonths.toFloat()).coerceIn(0f, 1f) else 0f
    val amountPaid = (loan.principalAmount - outstandingBalance).coerceAtLeast(0.0)
    val nextDueDate = remember(loan.startDate, monthsElapsed) {
        val startDate = Instant.ofEpochMilli(loan.startDate).atZone(ZoneId.systemDefault()).toLocalDate()
        startDate.plusMonths((monthsElapsed + 1).toLong())
    }
    val paymentHistory = remember(loan.emiAmount, loan.startDate, monthsElapsed) {
        buildPaymentHistory(loan.emiAmount, loan.startDate, monthsElapsed)
    }

    val dueDateFmt = remember { DateTimeFormatter.ofPattern("d MMM yyyy") }
    val startDateFmt = remember { DateTimeFormatter.ofPattern("d MMM yyyy") }
    val totalInterest = calcTotalInterest(loan)
    val totalPayment = loan.principalAmount + totalInterest
    val pctInterest = if (totalPayment > 0) (totalInterest / totalPayment * 100) else 0.0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .verticalScroll(rememberScrollState()),
    ) {
        // Gradient sub-header with loan type badge
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.linearGradient(listOf(Color(0xFF2563EB), Indigo600)))
                .padding(horizontal = 20.dp, vertical = 14.dp),
            contentAlignment = Alignment.Center,
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF1D4ED8).copy(alpha = 0.7f),
            ) {
                Text(
                    "${loan.loanType.displayName} · ${loan.interestRate}% p.a.",
                    fontSize = 11.sp,
                    color = Indigo100,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                )
            }
        }

        // Dark hero stat card
        Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Monthly EMI", fontSize = 11.sp, color = Color(0xFF94A3B8))
                        Spacer(Modifier.height(4.dp))
                        Text(
                            fmt.format(loan.emiAmount),
                            fontSize = 26.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF312E81),
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                            Text("Next Due", fontSize = 10.sp, color = Color(0xFF94A3B8))
                            Spacer(Modifier.height(2.dp))
                            Text(
                                nextDueDate.format(dueDateFmt),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFF87171),
                            )
                        }
                    }
                }
            }
        }

        // 2×2 info grid
        Column(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                InfoTile(
                    modifier = Modifier.weight(1f),
                    label = "Principal",
                    value = fmt.format(loan.principalAmount),
                )
                InfoTile(
                    modifier = Modifier.weight(1f),
                    label = "Outstanding",
                    value = fmt.format(outstandingBalance),
                    valueColor = UrgentRed,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                InfoTile(
                    modifier = Modifier.weight(1f),
                    label = "Rate of Interest",
                    value = "%.2f%% p.a.".format(loan.interestRate),
                )
                InfoTile(
                    modifier = Modifier.weight(1f),
                    label = "Tenure Remaining",
                    value = formatTenureRemaining(tenureRemainingMonths),
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        // Loan Progress bar
        Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Loan Progress", fontSize = 11.sp, color = Color(0xFF64748B))
                Spacer(Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Indigo100),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(fraction = progressFraction)
                            .background(Color(0xFF2563EB)),
                    )
                }
                Spacer(Modifier.height(6.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        "Paid ${formatLakh(amountPaid)} (${(progressFraction * 100).roundToInt()}%)",
                        fontSize = 10.sp,
                        color = Color(0xFF64748B),
                    )
                    Text(
                        "Remaining ${formatLakh(outstandingBalance)}",
                        fontSize = 10.sp,
                        color = Color(0xFF94A3B8),
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // EMI Payment History
        Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "EMI Payment History",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (paymentHistory.isEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "No payment history yet — first EMI due ${nextDueDate.format(dueDateFmt)}.",
                        fontSize = 12.sp,
                        color = Color(0xFF94A3B8),
                    )
                } else {
                    paymentHistory.forEach { entry ->
                        Spacer(Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                entry.label,
                                fontSize = 12.sp,
                                color = Color(0xFF64748B),
                                modifier = Modifier.weight(1f),
                            )
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (entry.isLate) Color(0xFFFEE2E2) else Color(0xFFDCFCE7),
                            ) {
                                Text(
                                    if (entry.isLate) "Late" else "Paid ✓",
                                    fontSize = 11.sp,
                                    color = if (entry.isLate) UrgentRed else SafeGreen,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Text(
                                fmt.format(entry.amount),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (entry.isLate) UrgentRed else MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // Loan details
        SectionLabel("LOAN DETAILS")
        Spacer(Modifier.height(6.dp))
        DetailCard {
            DetailRow(Icons.Default.AccountBalance, "Principal", fmt.format(loan.principalAmount))
            DetailRow(Icons.Default.Percent, "Interest Rate", "%.2f%% per annum".format(loan.interestRate))
            DetailRow(Icons.Default.Schedule, "Tenure", "${loan.tenureMonths} months")
            if (loan.bankName.isNotBlank()) DetailRow(Icons.Default.Business, "Bank / Lender", loan.bankName)
            if (loan.accountNumber.isNotBlank()) DetailRow(Icons.Default.CreditCard, "Account Number", "xxxx${loan.accountNumber.takeLast(4)}")
            val startDate = Instant.ofEpochMilli(loan.startDate).atZone(ZoneId.systemDefault()).toLocalDate()
            DetailRow(Icons.Default.CalendarToday, "Start Date", startDate.format(startDateFmt))
            if (loan.notes.isNotBlank()) DetailRow(Icons.Default.Note, "Notes", loan.notes)
        }

        Spacer(Modifier.height(12.dp))

        // Payment summary
        SectionLabel("PAYMENT SUMMARY")
        Spacer(Modifier.height(6.dp))
        DetailCard {
            DetailRow(Icons.Default.CurrencyRupee, "Total Principal", fmt.format(loan.principalAmount))
            DetailRow(Icons.Default.TrendingUp, "Total Interest", fmt.format(totalInterest))
            DetailRow(Icons.Default.Payments, "Total Payment", fmt.format(totalPayment), highlight = true)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Interest Burden", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(WarnOrange.copy(alpha = 0.12f))
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                ) {
                    Text("%.1f%%".format(pctInterest), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = WarnOrange)
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Action buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Button(
                onClick = { onViewAmortization(loan.principalAmount, loan.interestRate, loan.tenureMonths) },
                modifier = Modifier.weight(1f).height(50.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Indigo600),
            ) {
                Text("Amortize", fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
            Button(
                onClick = onPrepay,
                modifier = Modifier.weight(1f).height(50.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Indigo100, contentColor = Indigo600),
            ) {
                Text("Prepay", fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun InfoTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 14.dp)) {
            Text(label, fontSize = 11.sp, color = Color(0xFF64748B))
            Spacer(Modifier.height(6.dp))
            Text(value, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = valueColor)
        }
    }
}

@Composable
private fun DetailCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp), content = content)
    }
}

@Composable
private fun DetailRow(icon: ImageVector, label: String, value: String, highlight: Boolean = false) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = if (highlight) Indigo600 else Color(0xFF64748B), modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(8.dp))
        Text(label, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
        Text(
            value,
            fontSize = 13.sp,
            fontWeight = if (highlight) FontWeight.ExtraBold else FontWeight.Medium,
            color = if (highlight) Indigo600 else MaterialTheme.colorScheme.onSurface,
        )
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
        modifier = Modifier.padding(horizontal = 16.dp),
    )
}

private fun computeOutstandingBalance(
    principal: Double,
    annualRate: Double,
    tenureMonths: Int,
    monthsElapsed: Int,
): Double {
    if (monthsElapsed >= tenureMonths) return 0.0
    if (monthsElapsed <= 0) return principal
    val r = annualRate / (12.0 * 100.0)
    if (r == 0.0) return principal - (principal / tenureMonths * monthsElapsed)
    val n = tenureMonths.toDouble()
    val m = monthsElapsed.toDouble()
    return principal * ((1 + r).pow(n) - (1 + r).pow(m)) / ((1 + r).pow(n) - 1)
}

private fun formatTenureRemaining(months: Int): String {
    if (months <= 0) return "Completed"
    val years = months / 12
    val rem = months % 12
    return when {
        years > 0 && rem > 0 -> "$years yr $rem mo"
        years > 0 -> "$years yr"
        else -> "$rem mo"
    }
}

private fun formatLakh(amount: Double): String = when {
    amount >= 1_00_00_000 -> "₹${"%.1f".format(amount / 1_00_00_000)}Cr"
    amount >= 1_00_000 -> "₹${"%.1f".format(amount / 1_00_000)}L"
    else -> "₹${amount.roundToInt()}"
}

private fun buildPaymentHistory(
    emiAmount: Double,
    startDateMillis: Long,
    monthsElapsed: Int,
): List<EmiHistoryEntry> {
    if (monthsElapsed <= 0) return emptyList()
    val startDate = Instant.ofEpochMilli(startDateMillis).atZone(ZoneId.systemDefault()).toLocalDate()
    val monthFmt = DateTimeFormatter.ofPattern("MMM yyyy")
    val count = minOf(monthsElapsed, 5)
    return (monthsElapsed downTo (monthsElapsed - count + 1)).map { i ->
        val paymentDate = startDate.plusMonths(i.toLong())
        EmiHistoryEntry(
            label = paymentDate.format(monthFmt),
            isLate = false,
            amount = emiAmount,
        )
    }
}

private fun calcTotalInterest(loan: Loan): Double {
    val r = loan.interestRate / (12 * 100)
    if (r == 0.0) return 0.0
    val emi = (loan.principalAmount * r * (1 + r).pow(loan.tenureMonths)) / ((1 + r).pow(loan.tenureMonths) - 1)
    return (emi * loan.tenureMonths) - loan.principalAmount
}

private fun loanEmoji(type: String) = when (type.toLoanType()) {
    LoanType.HOME -> "🏠"; LoanType.CAR -> "🚗"; LoanType.PERSONAL -> "👤"
    LoanType.EDUCATION -> "🎓"; LoanType.BUSINESS -> "💼"; LoanType.OTHER -> "💰"
}
