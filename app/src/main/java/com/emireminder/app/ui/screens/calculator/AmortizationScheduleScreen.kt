package com.emireminder.app.ui.screens.calculator

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.emireminder.app.ui.theme.*
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private data class AmortRow(val month: Int, val emi: Double, val principal: Double, val interest: Double, val balance: Double)

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

    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val totalYears = (tenureMonths + 11) / 12
    val startDate = remember { LocalDate.now() }
    val monthFmt = DateTimeFormatter.ofPattern("MMM ''yy")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Amortization Schedule", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } },
                actions = {
                    IconButton(onClick = {
                        scope.launch { snackbarHostState.showSnackbar("Export feature coming soon") }
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
                            val firstMonthIdx = yearIdx * 12
                            // +2 offset: 1 for summary banner, 1 for chart, 1 for pills themselves
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(Indigo600)
                                    .clickable {
                                        scope.launch {
                                            // 4 header items before data rows
                                            listState.animateScrollToItem(4 + firstMonthIdx)
                                        }
                                    }
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

            // Data rows
            itemsIndexed(rows) { idx, row ->
                val bgColor = if (idx % 2 == 0) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.background
                val monthName = startDate.plusMonths(row.month.toLong()).format(monthFmt)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(bgColor)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Month: show year badge at start of each year, month name otherwise
                    Box(modifier = Modifier.width(72.dp), contentAlignment = Alignment.CenterStart) {
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
                            Text(monthName, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    TableCell(fmt(row.emi), Modifier.weight(1f), MaterialTheme.colorScheme.onSurface)
                    TableCell(fmt(row.principal), Modifier.weight(1f), Indigo600)
                    TableCell(fmt(row.interest), Modifier.weight(1f), WarnOrange)
                    TableCell(fmt(row.balance), Modifier.weight(1.3f))
                }
            }
        }
    }
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
