package com.emireminder.app.ui.screens.smsdashboard

import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.emireminder.app.ui.theme.*
import java.text.NumberFormat
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

private val rptAmtFmt = NumberFormat.getNumberInstance(Locale("en", "IN")).apply {
    maximumFractionDigits = 0
}

private fun rptFmt(sym: String, amount: Double): String =
    "$sym${rptAmtFmt.format(amount.toLong())}"

@Composable
fun MonthlyReportScreen(
    yearMonth: String,
    onBack: () -> Unit,
    viewModel: SmsDashboardViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Navigate the ViewModel to the target yearMonth when opened from a deep link or different month.
    LaunchedEffect(yearMonth) {
        val fmt = DateTimeFormatter.ofPattern("yyyy-MM")
        var safetyLimit = 24
        while (viewModel.selectedYearMonth.value != yearMonth && safetyLimit-- > 0) {
            val current = YearMonth.parse(viewModel.selectedYearMonth.value, fmt)
            val target = YearMonth.parse(yearMonth, fmt)
            when {
                current.isBefore(target) -> viewModel.nextMonth()
                current.isAfter(target)  -> viewModel.previousMonth()
                else -> break
            }
        }
    }

    val displayTitle = runCatching {
        val ym = YearMonth.parse(yearMonth, DateTimeFormatter.ofPattern("yyyy-MM"))
        "${ym.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${ym.year}"
    }.getOrElse { yearMonth }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Monthly Report", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text(displayTitle, fontSize = 12.sp, color = Color(0xFFBFCFE8))
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Indigo600,
                    titleContentColor = Color.White,
                ),
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
            when {
                uiState.isLoading -> item { RptLoadingContent() }

                !uiState.hasTransactions -> item { RptEmptyContent(month = displayTitle) }

                else -> {
                    item {
                        Spacer(Modifier.height(16.dp))
                        ReportSummarySection(
                            summary = uiState.summary,
                            currencySymbol = uiState.currencySymbol,
                        )
                    }

                    item {
                        Spacer(Modifier.height(20.dp))
                        Text(
                            "Category Breakdown",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Color(0xFF64748B),
                            modifier = Modifier.padding(horizontal = 16.dp),
                        )
                        Spacer(Modifier.height(8.dp))
                    }

                    val totalFlow = uiState.summary.totalIncome + uiState.summary.totalEmi + uiState.summary.totalExpenses

                    items(
                        items = uiState.categorySummaries,
                        key = { it.category.name },
                    ) { catSummary ->
                        ReportCategoryCard(
                            summary = catSummary,
                            currencySymbol = uiState.currencySymbol,
                            totalFlow = totalFlow,
                        )
                        Spacer(Modifier.height(8.dp))
                    }

                    item {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "${uiState.categorySummaries.sumOf { it.transactionCount }} transactions in $displayTitle",
                            fontSize = 12.sp,
                            color = Color(0xFF94A3B8),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }

            item {
                Spacer(Modifier.height(16.dp))
                BannerAdPlaceholder()
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun ReportSummarySection(summary: MonthlySummaryData, currencySymbol: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Slate800),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("Financial Overview", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF94A3B8))
            Spacer(Modifier.height(16.dp))

            RptSummaryRow(icon = Icons.Default.TrendingUp, iconColor = SafeGreen,
                label = "Total Income", value = rptFmt(currencySymbol, summary.totalIncome), valueColor = SafeGreen)
            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = Color(0xFF334155))
            RptSummaryRow(icon = Icons.Default.CreditCard, iconColor = Color(0xFF818CF8),
                label = "EMI & Loans", value = rptFmt(currencySymbol, summary.totalEmi), valueColor = Color(0xFFF87171))
            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = Color(0xFF334155))
            RptSummaryRow(icon = Icons.Default.ShoppingCart, iconColor = Amber700,
                label = "Total Expenses", value = rptFmt(currencySymbol, summary.totalExpenses), valueColor = Amber700)
            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = Color(0xFF334155))
            RptSummaryRow(icon = Icons.Default.Savings, iconColor = if (summary.netSavings >= 0) SafeGreen else UrgentRed,
                label = "Net Savings", value = rptFmt(currencySymbol, summary.netSavings),
                valueColor = if (summary.netSavings >= 0) SafeGreen else UrgentRed, bold = true)
        }
    }
}

@Composable
private fun RptSummaryRow(
    icon: ImageVector,
    iconColor: Color,
    label: String,
    value: String,
    valueColor: Color,
    bold: Boolean = false,
) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(iconColor.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(16.dp))
        }
        Spacer(Modifier.width(12.dp))
        Text(
            label,
            fontSize = 14.sp,
            color = Color(0xFFCBD5E1),
            fontWeight = if (bold) FontWeight.SemiBold else FontWeight.Normal,
            modifier = Modifier.weight(1f),
        )
        Text(
            value,
            fontSize = if (bold) 17.sp else 15.sp,
            fontWeight = if (bold) FontWeight.ExtraBold else FontWeight.SemiBold,
            color = valueColor,
        )
    }
}

@Composable
private fun ReportCategoryCard(
    summary: CategorySummary,
    currencySymbol: String,
    totalFlow: Double,
) {
    val meta = summary.category.meta()
    val fraction = if (totalFlow > 0) (summary.totalAmount / totalFlow).toFloat() else 0f

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(meta.color.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(meta.icon, contentDescription = null, tint = meta.color, modifier = Modifier.size(18.dp))
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
                Text(rptFmt(currencySymbol, summary.totalAmount), fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Slate800)
            }

            if (fraction > 0f) {
                Spacer(Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color(0xFFF1F5F9)),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fraction.coerceIn(0f, 1f))
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(2.dp))
                            .background(meta.color),
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text("${(fraction * 100).toInt()}% of total", fontSize = 10.sp, color = Color(0xFF94A3B8))
            }
        }
    }
}

@Composable
private fun RptLoadingContent() {
    Box(modifier = Modifier.fillMaxWidth().height(300.dp), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = Indigo600)
    }
}

@Composable
private fun RptEmptyContent(month: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(Icons.Default.BarChart, contentDescription = null, tint = Color(0xFFCBD5E1), modifier = Modifier.size(64.dp))
        Spacer(Modifier.height(16.dp))
        Text("No data for $month", fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = Slate800, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text("No bank SMS transactions were detected for this month.", fontSize = 14.sp, color = Color(0xFF94A3B8), textAlign = TextAlign.Center)
    }
}
