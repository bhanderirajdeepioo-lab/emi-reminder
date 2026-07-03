package com.emireminder.app.ui.screens.smsdashboard

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import com.emireminder.app.domain.model.TransactionCategory
import com.emireminder.app.domain.model.TransactionDirection
import com.emireminder.app.ui.theme.*
import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
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

// ─── Category filter order ─────────────────────────────────────────────────────

private val categoryFilterOrder = listOf(
    TransactionCategory.INCOME,
    TransactionCategory.EMI_AND_LOANS,
    TransactionCategory.CREDIT_CARD,
    TransactionCategory.UTILITIES,
    TransactionCategory.FOOD_AND_DINING,
    TransactionCategory.TRANSPORT,
    TransactionCategory.SHOPPING,
    TransactionCategory.HEALTH,
    TransactionCategory.ENTERTAINMENT,
    TransactionCategory.INVESTMENTS,
    TransactionCategory.INSURANCE,
    TransactionCategory.ATM_AND_CASH,
    TransactionCategory.BANK_CHARGES,
    TransactionCategory.UNCATEGORISED,
)

// ─── Date-grouped flat list helpers ───────────────────────────────────────────

private sealed interface TransactionListItem {
    data class DateHeader(val label: String) : TransactionListItem
    data class TxnCard(val txn: ParsedTransaction) : TransactionListItem
}

private fun buildTransactionListItems(transactions: List<ParsedTransaction>): List<TransactionListItem> {
    if (transactions.isEmpty()) return emptyList()
    val now = LocalDate.now()
    val yesterday = now.minusDays(1)
    val items = mutableListOf<TransactionListItem>()
    var lastDate: LocalDate? = null
    for (txn in transactions) {
        val date = Instant.ofEpochMilli(txn.transactionDate).atZone(ZoneId.systemDefault()).toLocalDate()
        if (date != lastDate) {
            val label = when (date) {
                now -> "Today"
                yesterday -> "Yesterday"
                else -> "${date.dayOfMonth} ${date.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())}"
            }
            items.add(TransactionListItem.DateHeader(label))
            lastDate = date
        }
        items.add(TransactionListItem.TxnCard(txn))
    }
    return items
}

// ─── Entry point ────────────────────────────────────────────────────────────

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun SmsDashboardScreen(
    onNavigateToFinanceToolsHub: () -> Unit,
    onNavigateToMonthlyReport: (String) -> Unit,
    onNavigateToFinanceAccounts: () -> Unit = {},
    onNavigateToScan: () -> Unit = {},
    onNavigateToTransactionDetail: (String) -> Unit = {},
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

    LaunchedEffect(uiState.smsHistoricalScanDone, uiState.isLoading) {
        if (!uiState.isLoading && !uiState.smsHistoricalScanDone) {
            onNavigateToScan()
        }
    }

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

    // Memoize the date-grouped list to avoid rebuilding on every recomposition
    val transactionListItems = remember(uiState.filteredTransactions) {
        buildTransactionListItems(uiState.filteredTransactions)
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
                        onScanCta = onNavigateToScan,
                    )
                }

                else -> {
                    // Account labelling prompt
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

                    // Category filter chips
                    item(key = "filter_chips") {
                        Spacer(Modifier.height(8.dp))
                        CategoryFilterChipBar(
                            categoriesWithTransactions = uiState.categoriesWithTransactions,
                            selectedFilter = uiState.selectedCategoryFilter,
                            onFilterSelected = viewModel::setCategoryFilter,
                        )
                    }

                    // Summary strip
                    item(key = "summary_strip") {
                        FilterSummaryStrip(
                            count = uiState.filteredTransactions.size,
                            totalAmount = uiState.filteredTransactions.sumOf { it.amount },
                            currencySymbol = uiState.currencySymbol,
                        )
                    }

                    // Empty filter state
                    val activeFilter = uiState.selectedCategoryFilter
                    if (uiState.filteredTransactions.isEmpty() && activeFilter != null) {
                        item(key = "empty_filter") {
                            EmptyFilterState(
                                categoryLabel = activeFilter.meta().label,
                                onClearFilter = { viewModel.setCategoryFilter(null) },
                            )
                        }
                    } else {
                        // Date-grouped smart card list
                        items(
                            items = transactionListItems,
                            key = { item ->
                                when (item) {
                                    is TransactionListItem.DateHeader -> "header_${item.label}"
                                    is TransactionListItem.TxnCard -> "txn_${item.txn.id}"
                                }
                            },
                        ) { item ->
                            when (item) {
                                is TransactionListItem.DateHeader -> DateGroupHeader(label = item.label)
                                is TransactionListItem.TxnCard -> {
                                    TransactionSmartCard(
                                        txn = item.txn,
                                        currencySymbol = uiState.currencySymbol,
                                        onClick = { onNavigateToTransactionDetail(item.txn.id) },
                                    )
                                    Spacer(Modifier.height(8.dp))
                                }
                            }
                        }
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

            Spacer(Modifier.height(12.dp))

            val progress = if (summary.totalIncome > 0)
                (summary.totalExpenses / summary.totalIncome).coerceIn(0.0, 1.0).toFloat()
            else 0f
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = Amber700,
                trackColor = Color(0xFF334155),
            )
            if (summary.netSavings > 0 && summary.totalIncome > 0) {
                val savingsPct = (summary.netSavings / summary.totalIncome * 100).toInt()
                Spacer(Modifier.height(4.dp))
                Text("💡 $savingsPct% saved", fontSize = 12.sp, color = SafeGreen)
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

// ─── Category filter chip bar ──────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryFilterChipBar(
    categoriesWithTransactions: Set<TransactionCategory>,
    selectedFilter: TransactionCategory?,
    onFilterSelected: (TransactionCategory?) -> Unit,
) {
    val visibleCategories = remember(categoriesWithTransactions) {
        categoryFilterOrder.filter { it in categoriesWithTransactions }
    }

    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item(key = "chip_all") {
            FilterChip(
                selected = selectedFilter == null,
                onClick = { onFilterSelected(null) },
                label = { Text("All", fontSize = 13.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Indigo600.copy(alpha = 0.15f),
                    selectedLabelColor = Indigo600,
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = selectedFilter == null,
                    selectedBorderColor = Indigo600,
                    borderColor = Color(0xFFE2E8F0),
                ),
            )
        }
        items(visibleCategories, key = { "chip_${it.name}" }) { cat ->
            val meta = cat.meta()
            val isSelected = selectedFilter == cat
            FilterChip(
                selected = isSelected,
                onClick = { onFilterSelected(if (isSelected) null else cat) },
                label = { Text(meta.label, fontSize = 13.sp) },
                leadingIcon = {
                    Icon(meta.icon, contentDescription = null, modifier = Modifier.size(16.dp))
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = meta.color.copy(alpha = 0.15f),
                    selectedLabelColor = meta.color,
                    selectedLeadingIconColor = meta.color,
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = isSelected,
                    selectedBorderColor = meta.color,
                    borderColor = Color(0xFFE2E8F0),
                ),
            )
        }
    }
}

// ─── Filter summary strip ──────────────────────────────────────────────────────

@Composable
private fun FilterSummaryStrip(count: Int, totalAmount: Double, currencySymbol: String) {
    Text(
        "$count transaction${if (count != 1) "s" else ""} · $currencySymbol${amtFmt.format(totalAmount.toLong())} total",
        fontSize = 12.sp,
        color = Color(0xFF94A3B8),
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 2.dp),
    )
}

// ─── Date group header ─────────────────────────────────────────────────────────

@Composable
private fun DateGroupHeader(label: String) {
    Text(
        label,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        color = Color(0xFF64748B),
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 16.dp, top = 12.dp, bottom = 4.dp),
    )
}

// ─── Transaction smart card ────────────────────────────────────────────────────

@Composable
private fun TransactionSmartCard(
    txn: ParsedTransaction,
    currencySymbol: String,
    onClick: () -> Unit,
) {
    val meta = txn.category.meta()
    val isCredit = txn.direction == TransactionDirection.CREDIT
    val prefix = if (isCredit) "+" else "-"
    val amtColor = if (isCredit) SafeGreen else UrgentRed
    val primaryLabel = txn.merchantName?.takeIf { it.isNotBlank() } ?: txn.bankName
    val subtitle = buildString {
        append(txn.bankName)
        if (txn.accountLast4.isNotBlank()) append(" ···· ${txn.accountLast4}")
        append(" · ")
        append(fmtDate(txn.transactionDate))
    }

    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
        ) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .fillMaxHeight()
                    .background(meta.color),
            )
            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            primaryLabel,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Slate800,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                        if (txn.userVerified) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = "Verified",
                                tint = SafeGreen,
                                modifier = Modifier.size(14.dp),
                            )
                        }
                        if (txn.isEmi) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Indigo600.copy(alpha = 0.12f))
                                    .padding(horizontal = 5.dp, vertical = 1.dp),
                            ) {
                                Text("EMI", fontSize = 9.sp, color = Indigo600, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    Text(subtitle, fontSize = 12.sp, color = Color(0xFF94A3B8))
                }

                Spacer(Modifier.width(8.dp))

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "$prefix${fmtAmt(currencySymbol, txn.amount)}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = amtColor,
                    )
                    Text(
                        if (isCredit) "CREDIT" else "DEBIT",
                        fontSize = 10.sp,
                        color = Color(0xFF94A3B8),
                    )
                }
            }
        }
    }
}

// ─── Empty filter state ────────────────────────────────────────────────────────

@Composable
private fun EmptyFilterState(categoryLabel: String, onClearFilter: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            Icons.Default.FilterList,
            contentDescription = null,
            tint = Color(0xFFCBD5E1),
            modifier = Modifier.size(48.dp),
        )
        Spacer(Modifier.height(12.dp))
        Text(
            "No $categoryLabel transactions this month.",
            fontSize = 14.sp,
            color = Color(0xFF64748B),
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(12.dp))
        TextButton(onClick = onClearFilter) {
            Text("Clear filter", color = Indigo600, fontWeight = FontWeight.SemiBold)
        }
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
private fun EmptyStateContent(month: String, onScanCta: () -> Unit = {}) {
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
            "No bank SMS detected${if (month.isNotEmpty()) " for $month" else ""}. Scan your messages to get started.",
            fontSize = 14.sp,
            color = Color(0xFF64748B),
            textAlign = TextAlign.Center,
            lineHeight = 22.sp,
        )
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onScanCta,
            colors = ButtonDefaults.buttonColors(containerColor = Indigo600),
        ) {
            Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Scan my bank SMS")
        }
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

@OptIn(ExperimentalMaterial3Api::class)
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
