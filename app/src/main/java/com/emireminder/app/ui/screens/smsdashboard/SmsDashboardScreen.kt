package com.emireminder.app.ui.screens.smsdashboard

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.background
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
import com.emireminder.app.ui.components.SwipeToEditDeleteRow
import kotlinx.coroutines.launch
import com.emireminder.app.ui.theme.*
import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

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

// Only these five chips are conditional — hidden when no transactions exist in the period.
// All other chips remain always-visible regardless of period data.
private val spendVerticalCategories = setOf(
    TransactionCategory.FOOD_AND_DINING,
    TransactionCategory.TRANSPORT,
    TransactionCategory.SHOPPING,
    TransactionCategory.HEALTH,
    TransactionCategory.BANK_CHARGES,
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
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(uiState.deleteError) {
        val error = uiState.deleteError ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message = error, duration = SnackbarDuration.Long)
        viewModel.clearDeleteError()
    }

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

    val transactionListItems = remember(uiState.filteredTransactions) {
        buildTransactionListItems(uiState.filteredTransactions)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Finance", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(
                            "Tracked from your SMS",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.7f),
                        )
                    }
                },
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
        snackbarHost = { SnackbarHost(snackbarHostState) },
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

                    // Smart Summary Card
                    item {
                        Spacer(Modifier.height(12.dp))
                        SmartSummaryCard(
                            summary = uiState.summary,
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

                    // Category summary card (only when a filter is active)
                    val activeFilter = uiState.selectedCategoryFilter
                    if (activeFilter != null) {
                        item(key = "category_summary") {
                            Spacer(Modifier.height(4.dp))
                            CategorySummaryCard(
                                category = activeFilter,
                                totalAmount = uiState.filteredTransactions.sumOf { it.amount },
                                transactionCount = uiState.filteredTransactions.size,
                                currencySymbol = uiState.currencySymbol,
                            )
                        }
                    }

                    // Empty filter state
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
                                    SwipeToEditDeleteRow(
                                        onEdit = { viewModel.openEditSheet(item.txn) },
                                        onDelete = {
                                            val txn = item.txn
                                            viewModel.deleteTransaction(txn)
                                            scope.launch {
                                                val result = snackbarHostState.showSnackbar(
                                                    message = "Transaction deleted",
                                                    actionLabel = "Undo",
                                                    duration = SnackbarDuration.Short,
                                                )
                                                if (result == SnackbarResult.ActionPerformed) {
                                                    viewModel.undoDeleteTransaction(txn)
                                                }
                                            }
                                        },
                                        verticalPadding = 0.dp,
                                    ) {
                                        Column {
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
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        IconButton(onClick = onPrev, modifier = Modifier.size(40.dp)) {
            Icon(Icons.Default.ChevronLeft, contentDescription = "Previous month", tint = Color.White)
        }
        Box(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White.copy(alpha = 0.15f))
                .padding(vertical = 4.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                displayText,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
            )
        }
        IconButton(
            onClick = onNext,
            enabled = canGoNext,
            modifier = Modifier.size(40.dp),
        ) {
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = "Next month",
                tint = if (canGoNext) Color.White else Color.White.copy(alpha = 0.3f),
            )
        }
    }
}

// ─── Smart Summary Card ────────────────────────────────────────────────────────

@Composable
private fun SmartSummaryCard(
    summary: MonthlySummaryData,
    currencySymbol: String,
    onViewReport: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Slate800),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Box {
            // Decorative accent circle (top-right)
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .align(Alignment.TopEnd)
                    .offset(x = 30.dp, y = (-30).dp)
                    .clip(CircleShape)
                    .background(Indigo600.copy(alpha = 0.3f)),
            )

            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "TOTAL SPEND",
                    fontSize = 9.sp,
                    color = Color.White.copy(alpha = 0.5f),
                    letterSpacing = 1.sp,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    fmtAmt(currencySymbol, summary.totalSpend),
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )

                Spacer(Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    MiniStatBox(
                        label = "INCOME",
                        value = fmtAmt(currencySymbol, summary.totalIncome),
                        valueColor = Color(0xFF4ADE80),
                        modifier = Modifier.weight(1f),
                    )
                    MiniStatBox(
                        label = "EMIs",
                        value = fmtAmt(currencySymbol, summary.totalEmi),
                        valueColor = Color(0xFF818CF8),
                        modifier = Modifier.weight(1f),
                    )
                    MiniStatBox(
                        label = "TRANSACTIONS",
                        value = "${summary.transactionCount}",
                        valueColor = Color.White,
                        modifier = Modifier.weight(1f),
                    )
                }

                Spacer(Modifier.height(12.dp))

                val progress = if (summary.totalIncome > 0)
                    (summary.totalSpend / summary.totalIncome).coerceIn(0.0, 1.0).toFloat()
                else 0f

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        "Spend vs Income",
                        fontSize = 10.sp,
                        color = Color.White.copy(alpha = 0.6f),
                    )
                    Text(
                        "${(progress * 100).toInt()}%",
                        fontSize = 10.sp,
                        color = Color.White.copy(alpha = 0.8f),
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                Spacer(Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = Color(0xFF818CF8),
                    trackColor = Color.White.copy(alpha = 0.1f),
                )

                Spacer(Modifier.height(6.dp))
                TextButton(
                    onClick = onViewReport,
                    contentPadding = PaddingValues(0.dp),
                ) {
                    Text(
                        "See full report →",
                        fontSize = 11.sp,
                        color = Color(0xFF818CF8),
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

@Composable
private fun MiniStatBox(
    label: String,
    value: String,
    valueColor: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White.copy(alpha = 0.08f))
            .padding(horizontal = 8.dp, vertical = 8.dp),
    ) {
        Column {
            Text(
                label,
                fontSize = 9.sp,
                color = Color.White.copy(alpha = 0.5f),
                letterSpacing = 0.5.sp,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                value,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = valueColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
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
        categoryFilterOrder.filter { cat ->
            cat !in spendVerticalCategories || cat in categoriesWithTransactions
        }
    }

    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item(key = "chip_all") {
            FilterChip(
                selected = selectedFilter == null,
                onClick = { onFilterSelected(null) },
                label = {
                    Text(
                        "All",
                        fontSize = 13.sp,
                        fontWeight = if (selectedFilter == null) FontWeight.SemiBold else FontWeight.Normal,
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = Color.White,
                    labelColor = Color(0xFF475569),
                    selectedContainerColor = Indigo600,
                    selectedLabelColor = Color.White,
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
                label = {
                    Text(
                        meta.label,
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    )
                },
                leadingIcon = {
                    Icon(
                        meta.icon,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = Color.White,
                    labelColor = Color(0xFF475569),
                    iconColor = Color(0xFF475569),
                    selectedContainerColor = meta.color,
                    selectedLabelColor = Color.White,
                    selectedLeadingIconColor = Color.White,
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

// ─── Category summary card (shown when a category filter is active) ────────────

@Composable
private fun CategorySummaryCard(
    category: TransactionCategory,
    totalAmount: Double,
    transactionCount: Int,
    currencySymbol: String,
) {
    val meta = category.meta()
    Card(
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
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(meta.color),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    meta.icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp),
                )
            }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    meta.label,
                    fontSize = 10.sp,
                    color = Color(0xFF94A3B8),
                )
                Text(
                    fmtAmt(currencySymbol, totalAmount),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = meta.color,
                )
            }
            Text(
                "$transactionCount transaction${if (transactionCount != 1) "s" else ""}",
                fontSize = 10.sp,
                color = Color(0xFF94A3B8),
            )
        }
    }
}

// ─── Date group header ─────────────────────────────────────────────────────────

@Composable
private fun DateGroupHeader(label: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 16.dp, top = 12.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            label.uppercase(),
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF94A3B8),
            letterSpacing = 0.5.sp,
        )
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            thickness = 1.dp,
            color = Color(0xFFE2E8F0),
        )
    }
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
                        .clip(RoundedCornerShape(10.dp))
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
                                    .clip(RoundedCornerShape(7.dp))
                                    .background(Indigo600.copy(alpha = 0.12f))
                                    .padding(horizontal = 5.dp, vertical = 2.dp),
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
                title = {
                    Column {
                        Text("Finance", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(
                            "Tracked from your SMS",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.7f),
                        )
                    }
                },
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
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
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
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
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
