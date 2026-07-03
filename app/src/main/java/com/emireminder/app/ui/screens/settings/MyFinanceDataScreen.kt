package com.emireminder.app.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.emireminder.app.data.db.entity.ParsedTransaction
import com.emireminder.app.domain.model.TransactionCategory
import com.emireminder.app.domain.model.TransactionDirection
import com.emireminder.app.ui.theme.*
import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val amtFmt = NumberFormat.getNumberInstance(Locale("en", "IN")).apply {
    maximumFractionDigits = 2
    minimumFractionDigits = 0
}

private fun fmtAmount(amount: Double): String = amtFmt.format(amount)

private fun fmtDate(epochMs: Long): String =
    DateTimeFormatter.ofPattern("dd MMM yyyy")
        .withZone(ZoneId.systemDefault())
        .format(Instant.ofEpochMilli(epochMs))

private fun TransactionCategory.displayLabel(): String = when (this) {
    TransactionCategory.INCOME          -> "Salary / Credit"
    TransactionCategory.EMI_AND_LOANS   -> "EMI Debit"
    TransactionCategory.FOOD_AND_DINING -> "Food & Dining"
    TransactionCategory.TRANSPORT       -> "Transport"
    TransactionCategory.UTILITIES       -> "Utility / Telecom"
    TransactionCategory.SHOPPING        -> "Shopping"
    TransactionCategory.HEALTH          -> "Health"
    TransactionCategory.ENTERTAINMENT   -> "Entertainment"
    TransactionCategory.INVESTMENTS     -> "Investment"
    TransactionCategory.INSURANCE       -> "Insurance"
    TransactionCategory.CREDIT_CARD     -> "Credit Card"
    TransactionCategory.ATM_AND_CASH    -> "ATM Withdrawal"
    TransactionCategory.BANK_CHARGES    -> "Bank Charges"
    TransactionCategory.UNCATEGORISED   -> "UPI / Transfer"
}

private fun TransactionCategory.accentColor(): Color = when (this) {
    TransactionCategory.INCOME          -> SafeGreen
    TransactionCategory.EMI_AND_LOANS   -> UrgentRed
    TransactionCategory.CREDIT_CARD     -> WarnOrange
    TransactionCategory.INVESTMENTS     -> Indigo600
    TransactionCategory.INSURANCE       -> Violet600
    TransactionCategory.ATM_AND_CASH    -> Color(0xFF6B7280)
    else                                -> HomeLoanColor
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyFinanceDataScreen(
    onBack: () -> Unit,
    viewModel: MyFinanceDataViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Finance Data", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Indigo600,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                ),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            // DPDP Act banner
            Surface(
                color = Indigo50,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Icon(
                        Icons.Default.Shield,
                        contentDescription = null,
                        tint = Indigo600,
                        modifier = Modifier.size(18.dp).padding(top = 1.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(
                            "DPDP Act 2023 — Right to Access",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Indigo600,
                        )
                        Text(
                            "All personal finance data derived from your SMS messages and stored on this device is listed below.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            when (val state = uiState) {
                is MyFinanceDataUiState.Loading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Indigo600)
                    }
                }

                is MyFinanceDataUiState.Empty -> {
                    EmptyState()
                }

                is MyFinanceDataUiState.Error -> {
                    ErrorState(state.message)
                }

                is MyFinanceDataUiState.Success -> {
                    val transactions = state.transactions
                    // Record count summary
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            "${transactions.size} stored record${if (transactions.size == 1) "" else "s"}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        )
                    }
                    HorizontalDivider()

                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(transactions, key = { it.id }) { txn ->
                            TransactionDataRow(txn)
                            HorizontalDivider(modifier = Modifier.padding(start = 72.dp))
                        }
                        item { Spacer(Modifier.height(24.dp)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun TransactionDataRow(txn: ParsedTransaction) {
    val isCredit = txn.direction == TransactionDirection.CREDIT
    val accent = txn.category.accentColor()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Category icon circle
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(accent.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                if (isCredit) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(20.dp),
            )
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                txn.category.displayLabel(),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                fmtDate(txn.transactionDate),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DataChip("••${txn.accountLast4}")
                DataChip(txn.senderId)
            }
        }

        Spacer(Modifier.width(8.dp))

        // Amount
        Column(horizontalAlignment = Alignment.End) {
            Text(
                "${if (isCredit) "+" else "−"}₹${fmtAmount(txn.amount)}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = if (isCredit) SafeGreen else MaterialTheme.colorScheme.onSurface,
                fontSize = 14.sp,
            )
            if (txn.isEmi) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = UrgentRed.copy(alpha = 0.1f),
                ) {
                    Text(
                        "EMI",
                        style = MaterialTheme.typography.labelSmall,
                        color = UrgentRed,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun DataChip(label: String) {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun EmptyState() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp),
        ) {
            Icon(
                Icons.Default.Storage,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(56.dp),
            )
            Spacer(Modifier.height(16.dp))
            Text(
                "No finance data stored",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Finance transactions derived from your SMS messages will appear here once SMS Intelligence is active.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }
    }
}

@Composable
private fun ErrorState(message: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp),
        ) {
            Icon(
                Icons.Default.ErrorOutline,
                contentDescription = null,
                tint = UrgentRed,
                modifier = Modifier.size(48.dp),
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "Unable to load data",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }
    }
}
