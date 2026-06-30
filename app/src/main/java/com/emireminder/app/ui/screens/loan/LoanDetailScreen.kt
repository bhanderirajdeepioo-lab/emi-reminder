package com.emireminder.app.ui.screens.loan

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.pow

@Composable
fun LoanDetailScreen(
    loanId: Int,
    onBack: () -> Unit,
    onViewAmortization: (Double, Double, Int) -> Unit,
    viewModel: LoanDetailViewModel = hiltViewModel(),
) {
    val loan by viewModel.loan.collectAsState()
    var showDeleteDialog by remember { mutableStateOf(false) }
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
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Default.Delete, "Delete Loan")
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
) {
    val totalInterest = calcTotalInterest(loan)
    val totalPayment = loan.principalAmount + totalInterest
    val pctInterest = if (totalPayment > 0) (totalInterest / totalPayment * 100) else 0.0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .verticalScroll(rememberScrollState()),
    ) {
        // Hero card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.linearGradient(listOf(Indigo600, Violet600)))
                .padding(20.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(56.dp).clip(RoundedCornerShape(16.dp)).background(Color.White.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(loanEmoji(loan.type), fontSize = 28.sp)
                }
                Spacer(Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(loan.name, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                    Text(
                        "${loan.loanType.displayName}${if (loan.bankName.isNotBlank()) " • ${loan.bankName}" else ""}",
                        fontSize = 13.sp,
                        color = Indigo100,
                    )
                }
            }
        }

        // EMI spotlight
        Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).offset(y = (-20).dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(20.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                EmiStatColumn("Monthly EMI", fmt.format(loan.emiAmount), Indigo600)
                Box(modifier = Modifier.width(1.dp).height(48.dp).background(MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)))
                EmiStatColumn("Rate", "%.1f%%".format(loan.interestRate), Color(0xFF0891B2))
                Box(modifier = Modifier.width(1.dp).height(48.dp).background(MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)))
                EmiStatColumn("Tenure", "${loan.tenureMonths} mo", SafeGreen)
            }
        }

        Column(modifier = Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            // Loan details
            SectionLabel("LOAN DETAILS")
            DetailCard {
                DetailRow(Icons.Default.AccountBalance, "Principal", fmt.format(loan.principalAmount))
                DetailRow(Icons.Default.Percent, "Interest Rate", "%.2f%% per annum".format(loan.interestRate))
                DetailRow(Icons.Default.Schedule, "Tenure", "${loan.tenureMonths} months")
                if (loan.bankName.isNotBlank()) DetailRow(Icons.Default.Business, "Bank / Lender", loan.bankName)
                if (loan.accountNumber.isNotBlank()) DetailRow(Icons.Default.CreditCard, "Account Number", "xxxx${loan.accountNumber.takeLast(4)}")
                val startDate = Instant.ofEpochMilli(loan.startDate).atZone(ZoneId.systemDefault()).toLocalDate()
                DetailRow(Icons.Default.CalendarToday, "Start Date", startDate.format(DateTimeFormatter.ofPattern("d MMM yyyy")))
                if (loan.notes.isNotBlank()) DetailRow(Icons.Default.Note, "Notes", loan.notes)
            }

            // Payment summary
            SectionLabel("PAYMENT SUMMARY")
            DetailCard {
                DetailRow(Icons.Default.CurrencyRupee, "Total Principal", fmt.format(loan.principalAmount))
                DetailRow(Icons.Default.TrendingUp, "Total Interest", fmt.format(totalInterest))
                DetailRow(Icons.Default.Payments, "Total Payment", fmt.format(totalPayment), highlight = true)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Interest Burden", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Box(
                        modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(WarnOrange.copy(alpha = 0.12f)).padding(horizontal = 8.dp, vertical = 3.dp),
                    ) {
                        Text("%.1f%%".format(pctInterest), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = WarnOrange)
                    }
                }
            }

            // Actions
            Button(
                onClick = { onViewAmortization(loan.principalAmount, loan.interestRate, loan.tenureMonths) },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Indigo600),
            ) {
                Icon(Icons.Default.TableChart, null)
                Spacer(Modifier.width(8.dp))
                Text("View Amortization Schedule", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun EmiStatColumn(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(4.dp))
        Text(value, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = color)
    }
}

@Composable
private fun DetailCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
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
        Text(value, fontSize = 13.sp, fontWeight = if (highlight) FontWeight.ExtraBold else FontWeight.Medium, color = if (highlight) Indigo600 else MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B), letterSpacing = 0.5.sp)
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
