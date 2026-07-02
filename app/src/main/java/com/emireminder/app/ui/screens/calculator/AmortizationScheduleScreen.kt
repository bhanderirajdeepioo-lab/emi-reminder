package com.emireminder.app.ui.screens.calculator

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.Dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import com.emireminder.app.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private data class AmortRow(val month: Int, val emi: Double, val principal: Double, val interest: Double, val balance: Double)

private data class YearSummary(val year: Int, val totalEmi: Double, val totalPrincipal: Double, val totalInterest: Double)

private sealed interface AmortItem {
    data class Row(val data: AmortRow, val isCurrentMonth: Boolean) : AmortItem
    data class YearEnd(val summary: YearSummary) : AmortItem
}

@Composable
fun AmortizationScheduleScreen(
    principal: Double,
    rate: Double,
    tenureMonths: Int,
    onBack: () -> Unit,
    viewModel: CalculatorViewModel = hiltViewModel(),
) {
    val emi = remember(principal, rate, tenureMonths) { viewModel.calculateEmi(principal, rate, tenureMonths) }
    val totalInterest = remember(emi, tenureMonths, principal) { viewModel.calculateTotalInterest(emi, tenureMonths, principal) }
    val numFmt = NumberFormat.getNumberInstance(Locale("en", "IN"))
    fun fmt(v: Double) = "₹${numFmt.format(v.toLong())}"

    val rows: List<AmortRow> = remember(principal, rate, tenureMonths) {
        val monthlyRate = rate / (12 * 100)
        var balance = principal
        (1..tenureMonths).map { month ->
            val interest = balance * monthlyRate
            val principalPaid = emi - interest
            balance -= principalPaid
            AmortRow(month, emi, principalPaid, interest, maxOf(0.0, balance))
        }
    }

    val context = LocalContext.current
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val totalYears = (tenureMonths + 11) / 12
    val startDate = remember { LocalDate.now() }
    val monthFmt = DateTimeFormatter.ofPattern("MMM ''yy")

    // Current month index (0-based) relative to schedule start
    val currentMonthIndex = remember {
        val today = LocalDate.now()
        val months = java.time.temporal.ChronoUnit.MONTHS.between(startDate, today).toInt()
        months.coerceIn(0, tenureMonths - 1)
    }

    // Build flat list: data rows interleaved with year-end summary cards
    val amortItems: List<AmortItem> = remember(rows, currentMonthIndex) {
        buildList {
            var yearEmi = 0.0; var yearPrincipal = 0.0; var yearInterest = 0.0
            rows.forEachIndexed { idx, row ->
                yearEmi += row.emi; yearPrincipal += row.principal; yearInterest += row.interest
                add(AmortItem.Row(row, idx == currentMonthIndex))
                if ((idx + 1) % 12 == 0 || idx == rows.lastIndex) {
                    val yearNum = idx / 12 + 1
                    add(AmortItem.YearEnd(YearSummary(yearNum, yearEmi, yearPrincipal, yearInterest)))
                    yearEmi = 0.0; yearPrincipal = 0.0; yearInterest = 0.0
                }
            }
        }
    }

    // Map year index -> position of first row in that year within amortItems (for jump pills)
    val yearFirstRowIndex: List<Int> = remember(amortItems) {
        (0 until totalYears).map { yearIdx ->
            val targetMonthIdx = yearIdx * 12
            amortItems.indexOfFirst { it is AmortItem.Row && it.data.month - 1 == targetMonthIdx }
                .takeIf { it >= 0 } ?: 0
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Amortization Schedule", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } },
                actions = {
                    IconButton(onClick = {
                        scope.launch {
                            try {
                                exportAmortizationCsv(context, principal, rate, rows, startDate, monthFmt)
                            } catch (e: Exception) {
                                snackbarHostState.showSnackbar("Export failed: ${e.message}")
                            }
                        }
                    }) {
                        Icon(Icons.Default.Download, "Export", tint = Color.White)
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
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 24.dp),
        ) {
            // Summary banner
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(Indigo600, Violet600))).padding(16.dp),
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        SummaryCol("Monthly EMI", fmt(emi))
                        SummaryCol("Total Interest", fmt(totalInterest))
                        SummaryCol("Total Amount", fmt(principal + totalInterest))
                    }
                }
            }

            // Interest vs Principal area chart
            item {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Interest vs Principal", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B), letterSpacing = 0.5.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        LegendDot(Indigo600, "Principal")
                        LegendDot(WarnOrange, "Interest")
                    }
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    ) {
                        InterestPrincipalAreaChart(rows = rows, modifier = Modifier.fillMaxWidth().height(140.dp).padding(8.dp))
                    }
                }
            }

            // Jump to year pills
            if (totalYears > 1) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        repeat(totalYears) { yearIdx ->
                            val yearLabel = "Y${yearIdx + 1}"
                            // +4 header items (banner, chart, pills row, header row) before amortItems
                            val targetPos = 4 + (yearFirstRowIndex.getOrElse(yearIdx) { yearIdx * 13 })
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(Indigo600)
                                    .clickable { scope.launch { listState.animateScrollToItem(targetPos) } }
                                    .padding(horizontal = 14.dp, vertical = 6.dp),
                            ) {
                                Text(yearLabel, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                }
            }

            // Header row
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                ) {
                    TableHeaderCell("Month", Modifier.width(72.dp))
                    TableHeaderCell("EMI", Modifier.weight(1f))
                    TableHeaderCell("Principal", Modifier.weight(1f))
                    TableHeaderCell("Interest", Modifier.weight(1f))
                    TableHeaderCell("Balance", Modifier.weight(1.3f))
                }
            }

            // Data rows + year-end summary cards
            itemsIndexed(amortItems) { _, item ->
                when (item) {
                    is AmortItem.Row -> {
                        val row = item.data
                        val isCurrent = item.isCurrentMonth
                        val monthName = startDate.plusMonths(row.month.toLong()).format(monthFmt)
                        val bgColor = when {
                            isCurrent -> Indigo50
                            else -> if (row.month % 2 == 0) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.background
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .then(if (isCurrent) Modifier.drawLeftBorder(Indigo600, 4.dp) else Modifier)
                                .background(bgColor)
                                .padding(horizontal = 16.dp, vertical = if (isCurrent) 10.dp else 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(modifier = Modifier.width(72.dp), contentAlignment = Alignment.CenterStart) {
                                Column {
                                    if (row.month % 12 == 1) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(Indigo600)
                                                    .padding(horizontal = 4.dp, vertical = 2.dp),
                                            ) {
                                                Text("Y${(row.month - 1) / 12 + 1}", fontSize = 9.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                            }
                                            Text(monthName, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    } else {
                                        Text(monthName, fontSize = 10.sp, color = if (isCurrent) Indigo600 else MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal)
                                    }
                                    if (isCurrent) {
                                        Text("Month ${row.month} of $tenureMonths", fontSize = 8.sp, color = Indigo600, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                            TableCell(fmt(row.emi), Modifier.weight(1f), if (isCurrent) Indigo600 else MaterialTheme.colorScheme.onSurface)
                            TableCell(fmt(row.principal), Modifier.weight(1f), Indigo600)
                            TableCell(fmt(row.interest), Modifier.weight(1f), WarnOrange)
                            TableCell(fmt(row.balance), Modifier.weight(1.3f))
                        }
                    }
                    is AmortItem.YearEnd -> {
                        val s = item.summary
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF1E293B))
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("Year ${s.year} Summary", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF94A3B8))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    YearSumCol("Total EMI", fmt(s.totalEmi), Color.White)
                                    YearSumCol("Interest", fmt(s.totalInterest), WarnOrange)
                                    YearSumCol("Principal", fmt(s.totalPrincipal), Color(0xFF6EE7B7))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private suspend fun exportAmortizationCsv(
    context: Context,
    principal: Double,
    rate: Double,
    rows: List<AmortRow>,
    startDate: LocalDate,
    monthFmt: DateTimeFormatter,
) {
    val dateFmt = DateTimeFormatter.ofPattern("dd MMM yyyy")
    val file = withContext(Dispatchers.IO) {
        val csv = buildString {
            appendLine("Month,Date,EMI Amount,Principal,Interest,Balance")
            rows.forEach { row ->
                val date = startDate.plusMonths(row.month.toLong()).format(dateFmt)
                appendLine("${row.month},$date,${String.format("%.2f", row.emi)},${String.format("%.2f", row.principal)},${String.format("%.2f", row.interest)},${String.format("%.2f", row.balance)}")
            }
        }
        val fileName = "amortization_${principal.toLong()}_${rate}.csv"
        File(context.cacheDir, fileName).also { it.writeText(csv) }
    }
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/csv"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_SUBJECT, "Amortization Schedule")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Export Amortization Schedule"))
}

@Composable
private fun InterestPrincipalAreaChart(rows: List<AmortRow>, modifier: Modifier) {
    val indigo = Indigo600
    val orange = WarnOrange
    if (rows.isEmpty()) return
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val n = rows.size
        val emiMax = rows.first().emi.toFloat()

        fun xOf(i: Int) = if (n > 1) w * i / (n - 1) else w / 2
        fun yOfPrincipal(i: Int) = h - (h * (rows[i].principal / emiMax)).toFloat()
        fun yOfInterest(i: Int) = h - (h * (rows[i].interest / emiMax)).toFloat()

        // Principal area (bottom fill)
        val principalPath = Path().apply {
            moveTo(0f, h)
            rows.forEachIndexed { i, _ -> lineTo(xOf(i), yOfPrincipal(i)) }
            lineTo(xOf(n - 1), h)
            close()
        }
        drawPath(principalPath, indigo.copy(alpha = 0.20f), style = Fill)

        val principalLine = Path().apply {
            rows.forEachIndexed { i, _ ->
                if (i == 0) moveTo(xOf(i), yOfPrincipal(i)) else lineTo(xOf(i), yOfPrincipal(i))
            }
        }
        drawPath(principalLine, indigo, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5.dp.toPx()))

        // Interest area (top fill)
        val interestPath = Path().apply {
            moveTo(0f, h)
            rows.forEachIndexed { i, _ -> lineTo(xOf(i), yOfInterest(i)) }
            lineTo(xOf(n - 1), h)
            close()
        }
        drawPath(interestPath, orange.copy(alpha = 0.18f), style = Fill)

        val interestLine = Path().apply {
            rows.forEachIndexed { i, _ ->
                if (i == 0) moveTo(xOf(i), yOfInterest(i)) else lineTo(xOf(i), yOfInterest(i))
            }
        }
        drawPath(interestLine, orange, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5.dp.toPx()))
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(modifier = Modifier.size(8.dp).clip(RoundedCornerShape(4.dp)).background(color))
        Text(label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SummaryCol(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, fontSize = 10.sp, color = Indigo100)
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
    }
}

@Composable
private fun TableHeaderCell(text: String, modifier: Modifier) {
    Text(
        text,
        modifier = modifier,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.End,
    )
}

@Composable
private fun TableCell(text: String, modifier: Modifier, color: Color = MaterialTheme.colorScheme.onSurface) {
    Text(text, modifier = modifier, fontSize = 10.sp, color = color, textAlign = TextAlign.End, maxLines = 1)
}

@Composable
private fun YearSumCol(label: String, value: String, valueColor: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, fontSize = 9.sp, color = Color(0xFF94A3B8))
        Text(value, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = valueColor)
    }
}

private fun Modifier.drawLeftBorder(color: Color, width: Dp): Modifier = drawBehind {
    drawRect(color = color, size = androidx.compose.ui.geometry.Size(width.toPx(), size.height))
}
