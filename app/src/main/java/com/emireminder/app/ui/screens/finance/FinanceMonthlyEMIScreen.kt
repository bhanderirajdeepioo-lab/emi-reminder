package com.emireminder.app.ui.screens.finance

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.emireminder.app.data.db.entity.Loan
import com.emireminder.app.ui.theme.*
import java.text.NumberFormat
import java.util.Calendar
import java.util.Locale

private enum class EmiFilter { ALL, PAID, UPCOMING }

private data class LoanEmiItem(
    val loan: Loan,
    val dueDay: Int,
    val statusColor: Color,
    val isPaid: Boolean,
    val isOverdue: Boolean,
)

private fun Loan.toDueDay(): Int {
    val cal = Calendar.getInstance().apply { timeInMillis = startDate }
    return cal.get(Calendar.DAY_OF_MONTH).coerceIn(1, 28)
}

private val _emiAmountFmt = NumberFormat.getNumberInstance(Locale("en", "IN")).apply {
    maximumFractionDigits = 0
}

private fun fmtAmt(amount: Double): String = "₹${_emiAmountFmt.format(amount.toLong())}"

private val MONTHS = listOf("Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec")

@Composable
fun FinanceMonthlyEMIScreen(
    onNavigateToLoanDetail: (Int) -> Unit,
    onNavigateToFinanceToolsHub: () -> Unit = {},
    viewModel: FinanceViewModel = hiltViewModel(),
) {
    val activeLoans by viewModel.activeLoans.collectAsState()
    val today = remember { Calendar.getInstance() }
    var selectedYear by remember { mutableStateOf(today.get(Calendar.YEAR)) }
    var selectedMonth by remember { mutableStateOf(today.get(Calendar.MONTH)) } // 0-indexed
    var selectedFilter by remember { mutableStateOf(EmiFilter.ALL) }

    val todayDay = remember { today.get(Calendar.DAY_OF_MONTH) }
    val todayMonth = remember { today.get(Calendar.MONTH) }
    val todayYear = remember { today.get(Calendar.YEAR) }

    // Inline month comparisons inside derivedStateOf so state reads on selectedMonth/selectedYear
    // are tracked directly — avoids plain-Boolean intermediates that derivedStateOf can't track.
    val emiItems by remember {
        derivedStateOf {
            val isCurrentMonth = selectedMonth == todayMonth && selectedYear == todayYear
            val isPastMonth = selectedYear < todayYear ||
                    (selectedYear == todayYear && selectedMonth < todayMonth)
            activeLoans.map { loan ->
                val dueDay = loan.toDueDay()
                val overdue = when {
                    isPastMonth -> true
                    isCurrentMonth -> dueDay < todayDay
                    else -> false
                }
                val paid = isPastMonth
                LoanEmiItem(
                    loan = loan,
                    dueDay = dueDay,
                    statusColor = when {
                        paid -> SafeGreen
                        overdue -> UrgentRed
                        else -> Indigo600
                    },
                    isPaid = paid,
                    isOverdue = overdue && !paid,
                )
            }.sortedBy { it.dueDay }
        }
    }

    val filteredItems by remember {
        derivedStateOf {
            when (selectedFilter) {
                EmiFilter.ALL -> emiItems
                EmiFilter.PAID -> emiItems.filter { it.isPaid }
                EmiFilter.UPCOMING -> emiItems.filter { !it.isPaid && !it.isOverdue }
            }
        }
    }

    val totalEmi by remember { derivedStateOf { emiItems.sumOf { it.loan.emiAmount } } }
    val paidEmi by remember { derivedStateOf { emiItems.filter { it.isPaid }.sumOf { it.loan.emiAmount } } }
    val paidPercent by remember { derivedStateOf { if (totalEmi > 0) (paidEmi / totalEmi * 100).toInt() else 0 } }

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
                        Icon(Icons.Filled.Calculate, contentDescription = "Finance Tools")
                    }
                },
            )
        },
        containerColor = Indigo50,
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 16.dp),
        ) {
            // Year selector
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Indigo600)
                        .padding(bottom = 12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF312E81))
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            "‹",
                            modifier = Modifier.clickable { selectedYear-- },
                            fontSize = 16.sp, color = Color(0xFFC7D2FE), fontWeight = FontWeight.Bold,
                        )
                        Text(
                            "$selectedYear  ▾",
                            fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFFC7D2FE),
                        )
                        Text(
                            "›",
                            modifier = Modifier.clickable { selectedYear++ },
                            fontSize = 16.sp, color = Color(0xFFC7D2FE), fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }

            // Month horizontal scroller
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White)
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 8.dp, vertical = 9.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    MONTHS.forEachIndexed { index, month ->
                        val isSelected = index == selectedMonth
                        val isToday = index == today.get(Calendar.MONTH) && selectedYear == today.get(Calendar.YEAR)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) Indigo600 else Indigo50)
                                .clickable { selectedMonth = index }
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    month,
                                    fontSize = 10.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) Color(0xFFC7D2FE) else Color(0xFF64748B),
                                )
                                if (isToday) {
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        todayDay.toString(),
                                        fontSize = 14.sp, fontWeight = FontWeight.ExtraBold,
                                        color = if (isSelected) Color.White else Indigo600,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Monthly summary card
            item {
                Spacer(Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Slate800)
                        .padding(16.dp),
                ) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "${MONTHS[selectedMonth]} $selectedYear — ${emiItems.size} Loans",
                                fontSize = 11.sp, color = Color(0xFF94A3B8),
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(fmtAmt(totalEmi), fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Paid", fontSize = 10.sp, color = Color(0xFF64748B))
                            Spacer(Modifier.height(4.dp))
                            Box(
                                modifier = Modifier
                                    .width(80.dp)
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(Color(0xFF374151))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .fillMaxWidth(fraction = paidPercent / 100f)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(SafeGreen)
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                            Text("${fmtAmt(paidEmi)} / ${fmtAmt(totalEmi)}", fontSize = 9.sp, color = Color(0xFF64748B))
                            Spacer(Modifier.height(4.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(SafeGreen)
                                    .padding(horizontal = 10.dp, vertical = 4.dp),
                            ) {
                                Text("$paidPercent% paid", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                }
            }

            // Filter chips
            item {
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    val paid = emiItems.count { it.isPaid }
                    val upcoming = emiItems.count { !it.isPaid && !it.isOverdue }
                    FilterChipItem("All (${emiItems.size})", selectedFilter == EmiFilter.ALL) { selectedFilter = EmiFilter.ALL }
                    FilterChipItem("Paid ($paid)", selectedFilter == EmiFilter.PAID) { selectedFilter = EmiFilter.PAID }
                    FilterChipItem("Upcoming ($upcoming)", selectedFilter == EmiFilter.UPCOMING) { selectedFilter = EmiFilter.UPCOMING }
                }
            }

            if (filteredItems.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                        Text(
                            if (activeLoans.isEmpty()) "No active loans. Add a loan from the Home tab." else "No EMIs match this filter.",
                            textAlign = TextAlign.Center,
                            color = Color(0xFF94A3B8),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            } else {
                item { Spacer(Modifier.height(8.dp)) }
                items(filteredItems, key = { it.loan.id }) { item ->
                    EmiRow(item = item, selectedMonth = selectedMonth, selectedYear = selectedYear, onClick = { onNavigateToLoanDetail(item.loan.id) })
                    Spacer(Modifier.height(8.dp))
                }
            }

            // Yearly bar chart
            item {
                Spacer(Modifier.height(4.dp))
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Total per Month", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Slate800)
                        Text(
                            "Annual outflow: ${fmtAmt(totalEmi * 12)}",
                            fontSize = 11.sp, color = Color(0xFF64748B),
                        )
                        Spacer(Modifier.height(12.dp))
                        YearlyBarChart(
                            selectedMonth = selectedMonth,
                            monthlyTotal = totalEmi,
                            modifier = Modifier.fillMaxWidth().height(100.dp),
                        )
                        Spacer(Modifier.height(4.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            MONTHS.forEach { m ->
                                Text(m.first().toString(), fontSize = 8.sp, color = Color(0xFF94A3B8))
                            }
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(8.dp)) }
        }
    }
}

@Composable
private fun FilterChipItem(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(13.dp))
            .background(if (selected) Indigo600 else Color.White)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 6.dp),
    ) {
        Text(label, fontSize = 12.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) Color.White else Color(0xFF64748B))
    }
}

@Composable
private fun EmiRow(item: LoanEmiItem, selectedMonth: Int, selectedYear: Int, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            // Colored left bar
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(item.statusColor, RoundedCornerShape(topStart = 14.dp, bottomStart = 14.dp))
                    .height(80.dp)
            )
            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Date badge
                Box(
                    modifier = Modifier
                        .size(width = 40.dp, height = 48.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(item.statusColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            MONTHS[selectedMonth].uppercase(),
                            fontSize = 8.sp, fontWeight = FontWeight.Bold, color = item.statusColor,
                        )
                        Text(
                            "${item.dueDay}",
                            fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = item.statusColor,
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(item.loan.name, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Slate800)
                    if (item.loan.bankName.isNotBlank()) {
                        Text(item.loan.bankName, fontSize = 12.sp, color = Color(0xFF64748B))
                    }
                    val emiNum = run {
                        val startCal = Calendar.getInstance().apply { timeInMillis = item.loan.startDate }
                        val startYear = startCal.get(Calendar.YEAR)
                        val startMonth = startCal.get(Calendar.MONTH)
                        (selectedYear - startYear) * 12 + (selectedMonth - startMonth) + 1
                    }.coerceIn(1, item.loan.tenureMonths)
                    Text("EMI #$emiNum of ${item.loan.tenureMonths}", fontSize = 11.sp, color = Color(0xFF64748B))
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(fmtAmt(item.loan.emiAmount), fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = Slate800)
                    Spacer(Modifier.height(4.dp))
                    val statusLabel = when {
                        item.isPaid -> "PAID ✓"
                        item.isOverdue -> "OVERDUE"
                        else -> "DUE ${MONTHS[selectedMonth]} ${item.dueDay}"
                    }
                    val statusBg = when {
                        item.isPaid -> Color(0xFFDCFCE7)
                        item.isOverdue -> Color(0xFFFEE2E2)
                        else -> Color(0xFFFFF7ED)
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(11.dp))
                            .background(statusBg)
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                    ) {
                        Text(statusLabel, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = item.statusColor)
                    }
                }
            }
        }
    }
}

@Composable
private fun YearlyBarChart(selectedMonth: Int, monthlyTotal: Double, modifier: Modifier = Modifier) {
    val indigo = Indigo600
    val indigoLight = Indigo100
    Canvas(modifier = modifier) {
        if (monthlyTotal <= 0) return@Canvas
        val w = size.width
        val h = size.height
        val barCount = 12
        val barWidth = w / (barCount * 1.8f)
        val spacing = w / barCount
        val maxH = h * 0.85f

        for (i in 0 until barCount) {
            val x = i * spacing + (spacing - barWidth) / 2
            // All months carry the same fixed EMI total; use uniform height with the
            // selected month standing taller so it reads as the active period.
            val barH = if (i == selectedMonth) maxH else maxH * 0.75f
            val color = if (i == selectedMonth) indigo else if (i < selectedMonth) indigoLight else Color(0xFFE0E7FF)
            drawRoundRect(
                color = color,
                topLeft = Offset(x, h - barH),
                size = Size(barWidth, barH),
                cornerRadius = CornerRadius(3.dp.toPx()),
            )
        }
    }
}
