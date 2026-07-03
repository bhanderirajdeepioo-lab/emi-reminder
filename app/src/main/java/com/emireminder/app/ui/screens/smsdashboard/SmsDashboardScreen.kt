package com.emireminder.app.ui.screens.smsdashboard

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.emireminder.app.data.db.entity.BankAccount
import com.emireminder.app.data.db.entity.ParsedTransaction
import com.emireminder.app.domain.model.TransactionDirection
import com.emireminder.app.ui.theme.*
import java.text.NumberFormat
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.abs

private val amtFmt = NumberFormat.getNumberInstance(Locale("en", "IN")).apply {
    maximumFractionDigits = 0
}

private fun fmtAmt(sym: String, amount: Double): String =
    "$sym${amtFmt.format(amount.toLong())}"

private fun fmtDate(epochMs: Long): String {
    val local = Instant.ofEpochMilli(epochMs).atZone(ZoneId.systemDefault()).toLocalDate()
    return "${local.dayOfMonth} ${local.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())}"
}

private fun displayMonth(yearMonth: String): String = runCatching {
    val ym = YearMonth.parse(yearMonth, DateTimeFormatter.ofPattern("yyyy-MM"))
    "${ym.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${ym.year}"
}.getOrElse { yearMonth }

// ─── Entry point ────────────────────────────────────────────────────────────

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun SmsDashboardScreen(
    onNavigateToFinanceToolsHub: () -> Unit,
    onNavigateToMonthlyReport: (String) -> Unit,
    onNavigateToFinanceAccounts: () -> Unit = {},
    viewModel: SmsDashboardViewModel = hiltViewModel(),
) {
    val smsPermissions = rememberMultiplePermissionsState(
        permissions = listOf(
            android.Manifest.permission.READ_SMS,
            android.Manifest.permission.RECEIVE_SMS,
        )
    )

    if (!smsPermissions.allPermissionsGranted) {
        SmsPermissionScreen(
            onNavigateToFinanceToolsHub = onNavigateToFinanceToolsHub,
            onRequestPermission = { smsPermissions.launchMultiplePermissionRequest() },
        )
        return
    }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    if (uiState.editingTransaction != null) {
        TransactionEditSheet(
            transaction = uiState.editingTransaction!!,
            isSaving = uiState.editSaveInProgress,
            saveError = uiState.editSaveError,
            onSave = { amount, category, subCat, merchant, notes, lender ->
                viewModel.saveTransaction(
                    id = uiState.editingTransaction!!.id,
                    amount = amount,
                    category = category,
                    subCategory = subCat,
                    merchantName = merchant,
                    notes = notes,
                    lenderName = lender,
                )
            },
            onDismiss = viewModel::dismissEditSheet,
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Finance", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Indigo600,
                    titleContentColor = Color.White,
                    actionIconContentColor = Color.White,
                ),
                actions = {
                    IconButton(onClick = onNavigateToFinanceToolsHub) {
                        Icon(Icons.Default.Calculate, contentDescription = "Finance Tools")
                    }
                },
            )
        },
        containerColor = Indigo50,
        contentWindowInsets = WindowInsets(0.dp),
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 8.dp),
        ) {
            // Month picker
            item {
                MonthPickerHeader(
                    displayText = if (uiState.selectedYearMonth.isNotEmpty())
                        displayMonth(uiState.selectedYearMonth) else "",
                    onPrev = viewModel::previousMonth,
                    onNext = viewModel::nextMonth,
                    canGoNext = uiState.selectedYearMonth.isNotEmpty() &&
                        YearMonth.parse(
                            uiState.selectedYearMonth,
                            DateTimeFormatter.ofPattern("yyyy-MM"),
                        ).plusMonths(1).let { !it.isAfter(YearMonth.now()) },
                )
            }

            when {
                uiState.isLoading -> item { LoadingContent() }

                !uiState.hasTransactions -> item {
                    EmptyStateContent(
                        month = if (uiState.selectedYearMonth.isNotEmpty())
                            displayMonth(uiState.selectedYearMonth) else "",
                    )
                }

                else -> {
                    // Account labelling prompt — shown first when a new account needs a label
                    if (uiState.accountsNeedingLabel.isNotEmpty()) {
                        item(key = "label_prompt") {
                            Spacer(Modifier.height(12.dp))
                            AccountLabelPromptCard(
                                account = uiState.accountsNeedingLabel.first(),
                                onPromptShown = viewModel::onPromptShown,
                                onLabel = { acc, label -> viewModel.applyLabel(acc, label) },
                                onSkip = viewModel::skipLabel,
                            )
                        }
                    }

                    item {
                        Spacer(Modifier.height(12.dp))
                        MonthlySummaryCard(
                            summary = uiState.summary,
                            previous = uiState.previousSummary,
                            currencySymbol = uiState.currencySymbol,
                            onViewReport = { onNavigateToMonthlyReport(uiState.selectedYearMonth) },
                        )
                    }

                    item {
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "By Category",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Color(0xFF64748B),
                            modifier = Modifier.padding(horizontal = 16.dp),
                        )
                        Spacer(Modifier.height(8.dp))
                    }

                    items(
                        items = uiState.categorySummaries,
                        key = { it.category.name },
                    ) { catSummary ->
                        CategoryRow(
                            summary = catSummary,
                            currencySymbol = uiState.currencySymbol,
                            onTransactionClick = viewModel::openEditSheet,
                        )
                        Spacer(Modifier.height(8.dp))
                    }

                    // By Account section
                    if (uiState.accountSummaries.isNotEmpty()) {
                        item {
                            Spacer(Modifier.height(8.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    "By Account",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = Color(0xFF64748B),
                                )
                                TextButton(
                                    onClick = onNavigateToFinanceAccounts,
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                ) {
                                    Text(
                                        "Manage",
                                        fontSize = 12.sp,
                                        color = Color(0xFF818CF8),
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                    Icon(
                                        Icons.Default.ChevronRight,
                                        contentDescription = null,
                                        tint = Color(0xFF818CF8),
                                        modifier = Modifier.size(14.dp),
                                    )
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                        }

                        items(
                            items = uiState.accountSummaries,
                            key = { it.account.id },
                        ) { acctSummary ->
                            AccountSummaryRow(summary = acctSummary, currencySymbol = uiState.currencySymbol)
                            Spacer(Modifier.height(8.dp))
                        }
                    }
                }
            }

            item {
                Spacer(Modifier.height(8.dp))
                BannerAdPlaceholder()
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

// ─── Month picker ─────────────────────────────────────────────────────────────

@Composable
private fun MonthPickerHeader(
    displayText: String,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    canGoNext: Boolean,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Indigo600)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        IconButton(onClick = onPrev) {
            Icon(Icons.Default.ChevronLeft, contentDescription = "Previous month", tint = Color.White)
        }
        Text(displayText, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
        IconButton(onClick = onNext, enabled = canGoNext) {
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = "Next month",
                tint = if (canGoNext) Color.White else Color.White.copy(alpha = 0.3f),
            )
        }
    }
}

// ─── Monthly summary card ──────────────────────────────────────────────────────

@Composable
private fun MonthlySummaryCard(
    summary: MonthlySummaryData,
    previous: MonthlySummaryData?,
    currencySymbol: String,
    onViewReport: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Slate800),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Monthly Summary", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF94A3B8))
                TextButton(
                    onClick = onViewReport,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                ) {
                    Text("View Report", fontSize = 12.sp, color = Color(0xFF818CF8), fontWeight = FontWeight.SemiBold)
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color(0xFF818CF8), modifier = Modifier.size(14.dp))
                }
            }

            Spacer(Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                SummaryMetric(
                    label = "Income",
                    value = fmtAmt(currencySymbol, summary.totalIncome),
                    current = summary.totalIncome,
                    previous = previous?.totalIncome,
                    higherIsBetter = true,
                    valueColor = SafeGreen,
                    modifier = Modifier.weight(1f),
                )
                SummaryMetric(
                    label = "EMIs",
                    value = fmtAmt(currencySymbol, summary.totalEmi),
                    current = summary.totalEmi,
                    previous = previous?.totalEmi,
                    higherIsBetter = false,
                    valueColor = Color(0xFFF87171),
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                SummaryMetric(
                    label = "Expenses",
                    value = fmtAmt(currencySymbol, summary.totalExpenses),
                    current = summary.totalExpenses,
                    previous = previous?.totalExpenses,
                    higherIsBetter = false,
                    valueColor = Amber700,
                    modifier = Modifier.weight(1f),
                )
                SummaryMetric(
                    label = "Net Savings",
                    value = fmtAmt(currencySymbol, summary.netSavings),
                    current = summary.netSavings,
                    previous = previous?.netSavings,
                    higherIsBetter = true,
                    valueColor = if (summary.netSavings >= 0) SafeGreen else UrgentRed,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun SummaryMetric(
    label: String,
    value: String,
    current: Double,
    previous: Double?,
    higherIsBetter: Boolean,
    valueColor: Color,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.padding(horizontal = 4.dp)) {
        Text(label, fontSize = 11.sp, color = Color(0xFF94A3B8))
        Spacer(Modifier.height(2.dp))
        Text(value, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = valueColor)
        if (previous != null && previous != 0.0) {
            val delta = current - previous
            val isPositive = delta > 0
            val isGood = isPositive == higherIsBetter
            Text(
                "${if (isPositive) "▲" else "▼"} ${fmtAmt("", abs(delta))}",
                fontSize = 10.sp,
                color = if (isGood) SafeGreen else UrgentRed,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

// ─── Category row ──────────────────────────────────────────────────────────────

@Composable
private fun CategoryRow(
    summary: CategorySummary,
    currencySymbol: String,
    onTransactionClick: (ParsedTransaction) -> Unit = {},
) {
    var expanded by remember { mutableStateOf(false) }
    val meta = summary.category.meta()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(meta.color.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(meta.icon, contentDescription = null, tint = meta.color, modifier = Modifier.size(20.dp))
                }

                Spacer(Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(meta.label, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Slate800)
                    Text(
                        "${summary.transactionCount} transaction${if (summary.transactionCount != 1) "s" else ""}",
                        fontSize = 12.sp,
                        color = Color(0xFF94A3B8),
                    )
                }

                Text(fmtAmt(currencySymbol, summary.totalAmount), fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Slate800)
                Spacer(Modifier.width(8.dp))
                Icon(
                    if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    tint = Color(0xFF94A3B8),
                    modifier = Modifier.size(20.dp),
                )
            }

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(),
                exit = shrinkVertically(),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF8FAFC)),
                ) {
                    HorizontalDivider(color = Color(0xFFF1F5F9))
                    summary.transactions.forEach { txn ->
                        TransactionRow(
                            transaction = txn,
                            currencySymbol = currencySymbol,
                            onClick = onTransactionClick,
                        )
                        HorizontalDivider(color = Color(0xFFF1F5F9))
                    }
                }
            }
        }
    }
}

@Composable
private fun TransactionRow(
    transaction: ParsedTransaction,
    currencySymbol: String,
    onClick: (ParsedTransaction) -> Unit = {},
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick(transaction) }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(
                    if (transaction.direction == TransactionDirection.CREDIT) SafeGreen else UrgentRed,
                ),
        )

        Spacer(Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val primaryLabel = transaction.merchantName?.takeIf { it.isNotBlank() }
                    ?: transaction.bankName
                Text(
                    primaryLabel,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = Slate800,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (transaction.userVerified) {
                    Spacer(Modifier.width(4.dp))
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = "Manually verified",
                        tint = SafeGreen,
                        modifier = Modifier.size(14.dp),
                    )
                }
            }
            val accountLabel = transaction.accountLast4
                .takeIf { it.isNotBlank() }
                ?.let { "${transaction.bankName} ·· $it" }
                ?: transaction.bankName
            Text(
                "${fmtDate(transaction.transactionDate)}  ·  $accountLabel",
                fontSize = 11.sp,
                color = Color(0xFF94A3B8),
            )
        }

        Spacer(Modifier.width(8.dp))

        val prefix = if (transaction.direction == TransactionDirection.CREDIT) "+" else "-"
        val amtColor = if (transaction.direction == TransactionDirection.CREDIT) SafeGreen else UrgentRed
        Text(
            "$prefix${fmtAmt(currencySymbol, transaction.amount)}",
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = amtColor,
        )
    }
}

// ─── State screens ─────────────────────────────────────────────────────────────

@Composable
private fun LoadingContent() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(color = Indigo600)
    }
}

@Composable
private fun EmptyStateContent(month: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(88.dp)
                .clip(CircleShape)
                .background(Indigo50),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Default.Inbox, contentDescription = null, tint = Indigo600, modifier = Modifier.size(44.dp))
        }
        Spacer(Modifier.height(20.dp))
        Text("No transactions found", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Slate800, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(
            "No bank SMS detected${if (month.isNotEmpty()) " for $month" else ""}. Make sure SMS monitoring is enabled.",
            fontSize = 14.sp,
            color = Color(0xFF64748B),
            textAlign = TextAlign.Center,
            lineHeight = 22.sp,
        )
    }
}

// ─── Account label prompt card ─────────────────────────────────────────────────

@Composable
private fun AccountLabelPromptCard(
    account: BankAccount,
    onPromptShown: (BankAccount) -> Unit,
    onLabel: (BankAccount, String) -> Unit,
    onSkip: (BankAccount) -> Unit,
) {
    var showCustomInput by remember { mutableStateOf(false) }
    var customText by remember { mutableStateOf("") }

    LaunchedEffect(account.id) {
        onPromptShown(account)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBEB)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFDE68A)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Default.AccountBalance, contentDescription = null, tint = Color(0xFF92400E), modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "New account detected",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF92400E),
                    )
                    Text(
                        "${account.bankName} account ·· ${account.accountLast4}",
                        fontSize = 12.sp,
                        color = Color(0xFFB45309),
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            Text(
                "What is this account?",
                fontSize = 13.sp,
                color = Color(0xFF78350F),
                fontWeight = FontWeight.Medium,
            )

            if (showCustomInput) {
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = customText,
                    onValueChange = { customText = it },
                    placeholder = { Text("e.g. Business Account", fontSize = 13.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFD97706),
                        unfocusedBorderColor = Color(0xFFFCD34D),
                    ),
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                    TextButton(onClick = { showCustomInput = false; customText = "" }) {
                        Text("Cancel", color = Color(0xFF6B7280))
                    }
                    Spacer(Modifier.width(4.dp))
                    Button(
                        onClick = { onLabel(account, customText.trim()) },
                        enabled = customText.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
                    ) {
                        Text("Save")
                    }
                }
            } else {
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    QuickLabelChip("Salary Account", onClick = { onLabel(account, "Salary Account") }, modifier = Modifier.weight(1f))
                    QuickLabelChip("Savings Account", onClick = { onLabel(account, "Savings Account") }, modifier = Modifier.weight(1f))
                }
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedButton(
                        onClick = { showCustomInput = true },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFD97706)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFCD34D)),
                    ) {
                        Text("Other…", fontSize = 12.sp)
                    }
                    OutlinedButton(
                        onClick = { onSkip(account) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF6B7280)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE5E7EB)),
                    ) {
                        Text("Skip", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickLabelChip(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
    ) {
        Text(label, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

// ─── Account summary row ────────────────────────────────────────────────────────

@Composable
private fun AccountSummaryRow(summary: AccountSummary, currencySymbol: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Indigo600.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.AccountBalance, contentDescription = null, tint = Indigo600, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(summary.displayName, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Slate800)
                Text(
                    "${summary.transactionCount} transaction${if (summary.transactionCount != 1) "s" else ""}",
                    fontSize = 12.sp,
                    color = Color(0xFF94A3B8),
                )
            }
            Text(fmtAmt(currencySymbol, summary.totalAmount), fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Slate800)
        }
    }
}

// ─── SMS Permission gate ────────────────────────────────────────────────────────

@Composable
private fun SmsPermissionScreen(
    onNavigateToFinanceToolsHub: () -> Unit,
    onRequestPermission: () -> Unit,
) {
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Finance", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Indigo600,
                    titleContentColor = Color.White,
                    actionIconContentColor = Color.White,
                ),
                actions = {
                    IconButton(onClick = onNavigateToFinanceToolsHub) {
                        Icon(Icons.Default.Calculate, contentDescription = "Finance Tools")
                    }
                },
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0.dp),
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(Indigo50),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.AccountBalance, contentDescription = null, tint = Indigo600, modifier = Modifier.size(48.dp))
            }

            Spacer(Modifier.height(24.dp))

            Text("Finance Intelligence", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = Slate800, textAlign = TextAlign.Center)
            Spacer(Modifier.height(8.dp))
            Text(
                "Grant SMS access to automatically detect your income, expenses, and EMIs from bank messages.",
                fontSize = 14.sp,
                color = Color(0xFF64748B),
                textAlign = TextAlign.Center,
                lineHeight = 22.sp,
            )

            Spacer(Modifier.height(32.dp))

            Button(
                onClick = onRequestPermission,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Indigo600),
            ) {
                Icon(Icons.Default.Lock, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Enable SMS Intelligence", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            }

            Spacer(Modifier.height(12.dp))

            OutlinedButton(
                onClick = {
                    context.startActivity(
                        Intent(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.fromParts("package", context.packageName, null),
                        )
                    )
                },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Indigo600),
            ) {
                Text("Open App Settings", fontSize = 14.sp)
            }
        }
    }
}

// ─── AdMob banner stub ──────────────────────────────────────────────────────────

@Composable
fun BannerAdPlaceholder(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(56.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFFF1F5F9)),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(Icons.Default.Campaign, contentDescription = null, tint = Color(0xFFCBD5E1), modifier = Modifier.size(18.dp))
            Text("Advertisement", fontSize = 12.sp, color = Color(0xFFCBD5E1), fontWeight = FontWeight.Medium)
        }
    }
}
