package com.emireminder.app.ui.screens.loan

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
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
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.atan2
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

private data class DonutSlice(
    val loanType: LoanType,
    val color: Color,
    val emiAmount: Double,
    val sweepAngle: Float,
)

private enum class AnalyticsPeriod(val label: String, val months: Int?) {
    THREE_M("3M", 3), SIX_M("6M", 6), ONE_Y("12M", 12), ALL("All", null)
}

@Composable
fun LoanAnalyticsScreen(
    onBack: () -> Unit,
    onNavigateToLoanDetail: (Int) -> Unit = {},
    onNavigateToReminders: () -> Unit = {},
    onNavigateToPrepayment: (loanId: Int) -> Unit = {},
    viewModel: LoanAnalyticsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val loans = uiState.loans
    val fmt = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
    var period by remember { mutableStateOf(AnalyticsPeriod.ONE_Y) }

    val totalEmi = uiState.totalEmi
    val totalPrincipal = uiState.totalPrincipal
    val totalInterest = uiState.totalInterest
    val totalPaid = uiState.totalPaid
    val remainingInterest = uiState.remainingInterest
    val today = remember { LocalDate.now() }
    val byCategory = uiState.byCategory

    data class LoanForecast(val loan: Loan, val remainingMonths: Int, val interestSavings: Double)
    val topForecastLoan: LoanForecast? = remember(loans) {
        loans.mapNotNull { loan ->
            val startDate = Instant.ofEpochMilli(loan.startDate).atZone(ZoneId.systemDefault()).toLocalDate()
            val monthsElapsed = ChronoUnit.MONTHS.between(startDate, today).toInt().coerceIn(0, loan.tenureMonths)
            val remaining = loan.tenureMonths - monthsElapsed
            if (remaining <= 0) return@mapNotNull null
            val r = loan.interestRate / (12 * 100)
            var balance = loan.principalAmount
            repeat(monthsElapsed) {
                val interest = balance * r
                balance -= (loan.emiAmount - interest)
            }
            balance = maxOf(0.0, balance)
            val savings = if (r > 0) {
                val remEmi = (balance * r * (1 + r).pow(remaining)) / ((1 + r).pow(remaining) - 1)
                (remEmi * remaining) - balance
            } else 0.0
            LoanForecast(loan, remaining, savings)
        }.maxByOrNull { it.remainingMonths }
    }

    val loansForType = remember(loans) { loans.groupBy { it.loanType } }

    val donutSlices = remember(loans) {
        val total = loans.sumOf { it.emiAmount }
        loans.groupBy { it.loanType }
            .entries
            .sortedByDescending { it.value.sumOf { l -> l.emiAmount } }
            .map { (type, typeLoans) ->
                val emi = typeLoans.sumOf { it.emiAmount }
                DonutSlice(
                    loanType = type,
                    color = loanTypeColor(type),
                    emiAmount = emi,
                    sweepAngle = if (total > 0) (emi / total * 360f).toFloat() else 360f,
                )
            }
    }

    val categoryColors = listOf(HomeLoanColor, CarLoanColor, PersonalLoanColor, OtherLoanColor, Indigo600, Violet600, Color(0xFF059669), WarnOrange)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Loan Analytics", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Indigo600, titleContentColor = Color.White, navigationIconContentColor = Color.White),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        if (loans.isEmpty()) {
            EmptyAnalytics(modifier = Modifier.fillMaxSize().padding(padding))
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(bottom = 24.dp),
            ) {
                // Period toggle
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        AnalyticsPeriod.entries.forEach { p ->
                            val selected = p == period
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(if (selected) Indigo600 else MaterialTheme.colorScheme.surface)
                                    .clickable { period = p }
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    p.label,
                                    fontSize = 13.sp,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }

                // Stacked bar chart: monthly EMI outflow by category
                item {
                    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                        SectionLabel("MONTHLY EMI OUTFLOW")
                        Spacer(Modifier.height(8.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    byCategory.forEachIndexed { i, (type, _) ->
                                        LegendDot(
                                            color = categoryColors.getOrElse(i) { Indigo600 },
                                            label = type.lowercase().replaceFirstChar { it.uppercase() },
                                        )
                                    }
                                }
                                StackedBarChart(
                                    loans = loans,
                                    period = period,
                                    colors = categoryColors,
                                    modifier = Modifier.fillMaxWidth().height(120.dp),
                                )
                            }
                        }
                    }
                }

                // Summary row: 3 inline cards
                item {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        SectionLabel("PORTFOLIO SUMMARY")
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            InlineSummaryCard(Modifier.weight(1f), "Total Paid", fmt.format(totalPaid.toLong()), Indigo600)
                            InlineSummaryCard(Modifier.weight(1f), "Monthly EMI", fmt.format(totalEmi.toLong()), Color(0xFF0891B2))
                            InlineSummaryCard(Modifier.weight(1f), "Remaining", fmt.format(remainingInterest.toLong()), WarnOrange)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            InlineSummaryCard(Modifier.weight(1f), "Total Loans", "${loans.size}", SafeGreen)
                            InlineSummaryCard(Modifier.weight(1f), "Principal", "₹${formatLakh(totalPrincipal)}", Indigo600)
                            InlineSummaryCard(Modifier.weight(1f), "Interest Est.", "₹${formatLakh(totalInterest)}", WarnOrange)
                        }
                    }
                }

                // Donut chart: Portfolio by loan type
                item {
                    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                        SectionLabel("PORTFOLIO BY LOAN TYPE")
                        Spacer(Modifier.height(8.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                LoanTypeDonut(
                                    slices = donutSlices,
                                    onSliceTapped = { tappedType ->
                                        val typeLoans = loansForType[tappedType] ?: return@LoanTypeDonut
                                        if (typeLoans.size == 1) {
                                            onNavigateToLoanDetail(typeLoans.first().id)
                                        } else {
                                            onNavigateToReminders()
                                        }
                                    },
                                    modifier = Modifier.size(140.dp),
                                )
                                Spacer(Modifier.width(16.dp))
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    donutSlices.forEach { slice ->
                                        LoanTypeDonutLegendRow(
                                            type = slice.loanType,
                                            emi = slice.emiAmount,
                                            total = totalEmi,
                                            color = slice.color,
                                            fmt = fmt,
                                        )
                                    }
                                    Divider()
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                    ) {
                                        Text("Total EMI", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                        Text(fmt.format(totalEmi.toLong()), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }

                // Category breakdown
                if (byCategory.isNotEmpty()) {
                    item {
                        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                            SectionLabel("CATEGORY BREAKDOWN")
                            Spacer(Modifier.height(8.dp))
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            ) {
                                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    byCategory.forEachIndexed { i, (type, emi) ->
                                        CategoryBar(
                                            type = type.lowercase().replaceFirstChar { it.uppercase() },
                                            emi = emi,
                                            total = totalEmi,
                                            color = categoryColors.getOrElse(i) { Indigo600 },
                                            fmt = fmt,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Foreclosure insight card (loan-specific)
                if (topForecastLoan != null) {
                    item {
                        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                            SectionLabel("FORECLOSURE INSIGHT")
                            Spacer(Modifier.height(8.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Brush.linearGradient(listOf(Color(0xFF1E1B4B), Color(0xFF312E81))))
                                    .padding(20.dp),
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(topForecastLoan.loan.name, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                            Text("has ${topForecastLoan.remainingMonths} EMIs left", fontSize = 12.sp, color = Color.White.copy(alpha = 0.7f))
                                        }
                                        Box(
                                            modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(Color(0xFF059669).copy(alpha = 0.25f)).padding(horizontal = 10.dp, vertical = 6.dp),
                                        ) {
                                            Text("INSIGHT", fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF6EE7B7))
                                        }
                                    }
                                    Divider(color = Color.White.copy(alpha = 0.15f))
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Column {
                                            Text("Foreclosure saves", fontSize = 11.sp, color = Color.White.copy(alpha = 0.6f))
                                            Text(fmt.format(topForecastLoan.interestSavings.toLong()), fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF6EE7B7))
                                        }
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text("EMIs remaining", fontSize = 11.sp, color = Color.White.copy(alpha = 0.6f))
                                            Text("${topForecastLoan.remainingMonths} months", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFA5B4FC))
                                        }
                                    }
                                    Button(
                                        onClick = { topForecastLoan?.let { onNavigateToPrepayment(it.loan.id) } },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5)),
                                        shape = RoundedCornerShape(10.dp),
                                        contentPadding = PaddingValues(vertical = 10.dp),
                                    ) {
                                        Text("View Prepayment Calculator", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                                    }
                                }
                            }
                        }
                    }
                }

                // Loan list
                item {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                        SectionLabel("ALL ACTIVE LOANS")
                    }
                }
                items(loans, key = { it.id }) { loan ->
                    LoanAnalyticsRow(loan = loan, fmt = fmt)
                }
            }
        }
    }
}

@Composable
private fun LoanTypeDonut(
    slices: List<DonutSlice>,
    onSliceTapped: (LoanType) -> Unit,
    modifier: Modifier,
) {
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    val slicesState by rememberUpdatedState(slices)

    Canvas(
        modifier = modifier
            .onSizeChanged { canvasSize = it }
            .pointerInput(Unit) {
                detectTapGestures { tapOffset ->
                    val w = canvasSize.width.toFloat()
                    val h = canvasSize.height.toFloat()
                    if (w == 0f || h == 0f) return@detectTapGestures
                    val cx = w / 2f
                    val cy = h / 2f
                    val strokeW = min(w, h) * 0.18f
                    val inset = strokeW / 2 + 4.dp.toPx()
                    val radius = min(w, h) / 2f - inset
                    val outerR = radius + strokeW / 2
                    val innerR = (radius - strokeW / 2).coerceAtLeast(0f)

                    val dx = tapOffset.x - cx
                    val dy = tapOffset.y - cy
                    val dist = sqrt(dx * dx + dy * dy)
                    if (dist < innerR || dist > outerR) return@detectTapGestures

                    // angle from positive-x axis, clockwise positive (screen coords)
                    var tapAngleDeg = (atan2(dy.toDouble(), dx.toDouble()) * (180.0 / Math.PI)).toFloat()
                    if (tapAngleDeg < 0f) tapAngleDeg += 360f

                    // re-zero relative to donut start at 270° (12 o'clock = -90° = 270°)
                    val relAngle = (tapAngleDeg - 270f + 360f) % 360f

                    var cumSweep = 0f
                    for (slice in slicesState) {
                        if (relAngle >= cumSweep && relAngle < cumSweep + slice.sweepAngle) {
                            onSliceTapped(slice.loanType)
                            break
                        }
                        cumSweep += slice.sweepAngle
                    }
                }
            },
    ) {
        val strokeWidth = size.minDimension * 0.18f
        val stroke = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
        val inset = strokeWidth / 2 + 4.dp.toPx()
        val arcSize = Size(size.width - inset * 2, size.height - inset * 2)
        val topLeft = Offset(inset, inset)

        // Background track
        drawArc(
            color = Color(0xFFEEF2FF),
            startAngle = -90f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = stroke,
        )

        var startAngle = -90f
        slices.forEach { slice ->
            drawArc(
                color = slice.color,
                startAngle = startAngle,
                sweepAngle = slice.sweepAngle,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = stroke,
            )
            startAngle += slice.sweepAngle
        }
    }
}

@Composable
private fun LoanTypeDonutLegendRow(type: LoanType, emi: Double, total: Double, color: Color, fmt: NumberFormat) {
    val pct = if (total > 0) "%.0f".format(emi / total * 100) else "0"
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(5.dp))
        Text(
            text = type.displayName,
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
            maxLines = 1,
        )
        Spacer(Modifier.width(4.dp))
        Column(horizontalAlignment = Alignment.End) {
            Text(fmt.format(emi.toLong()), fontSize = 10.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
            Text("$pct%", fontSize = 9.sp, color = color, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun StackedBarChart(loans: List<Loan>, period: AnalyticsPeriod, colors: List<Color>, modifier: Modifier) {
    val today = remember { LocalDate.now() }

    val categoryOrder = remember(loans) {
        loans.groupBy { it.type }
            .entries.sortedByDescending { it.value.sumOf { l -> l.emiAmount } }
            .map { it.key }
    }

    // Each entry: (month 1st-day, emi-per-category) — only loans active that month count
    val bars: List<Pair<LocalDate, List<Float>>> = remember(loans, period) {
        val endMonth = today.withDayOfMonth(1)
        val startMonth = when (val m = period.months) {
            null -> {
                val earliest = loans.minOfOrNull { it.startDate }
                    ?.let { Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate().withDayOfMonth(1) }
                    ?: endMonth.minusMonths(11)
                if (earliest.isBefore(endMonth.minusMonths(35))) endMonth.minusMonths(35) else earliest
            }
            else -> endMonth.minusMonths((m - 1).toLong())
        }
        val result = mutableListOf<Pair<LocalDate, List<Float>>>()
        var month = startMonth
        while (!month.isAfter(endMonth)) {
            val amounts = categoryOrder.map { type ->
                loans.filter { loan ->
                    if (loan.type != type) return@filter false
                    val loanStart = Instant.ofEpochMilli(loan.startDate)
                        .atZone(ZoneId.systemDefault()).toLocalDate().withDayOfMonth(1)
                    val loanEnd = loanStart.plusMonths(loan.tenureMonths.toLong())
                    !loanStart.isAfter(month) && month.isBefore(loanEnd)
                }.sumOf { it.emiAmount }.toFloat()
            }
            result.add(month to amounts)
            month = month.plusMonths(1)
        }
        result
    }

    val maxTotal = (bars.maxOfOrNull { it.second.sum() } ?: 1f).coerceAtLeast(1f)
    val textMeasurer = rememberTextMeasurer()

    Canvas(modifier = modifier) {
        if (bars.isEmpty()) return@Canvas
        val barCount = bars.size
        val labelAreaH = with(drawContext.density) { 18.dp.toPx() }
        val w = size.width
        val h = size.height - labelAreaH
        val barWidth = (w / barCount * 0.65f).coerceAtLeast(2f)
        val gap = w / barCount - barWidth
        val showLabels = barWidth >= 18.dp.toPx()

        bars.forEachIndexed { barIdx, (_, amounts) ->
            val x = gap / 2 + barIdx * (barWidth + gap)
            var yBottom = h
            val total = amounts.sum()

            amounts.forEachIndexed { catIdx, catEmi ->
                if (catEmi <= 0f) return@forEachIndexed
                val barH = h * catEmi / maxTotal
                val color = colors.getOrElse(catIdx) { Indigo600 }
                drawRect(
                    color = color,
                    topLeft = Offset(x, yBottom - barH),
                    size = Size(barWidth, barH),
                )
                yBottom -= barH
            }

            if (showLabels && total > 0f) {
                val label = "₹${formatLakh(total.toDouble())}"
                val measured = textMeasurer.measure(
                    text = label,
                    style = TextStyle(fontSize = 9.sp, color = Color(0xFF64748B)),
                )
                drawText(
                    textLayoutResult = measured,
                    topLeft = Offset(
                        x = (x + barWidth / 2 - measured.size.width / 2).coerceAtLeast(0f),
                        y = (yBottom - measured.size.height - 2.dp.toPx()).coerceAtLeast(0f),
                    ),
                )
            }
        }
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
private fun InlineSummaryCard(modifier: Modifier, label: String, value: String, color: Color) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = color, maxLines = 1)
        }
    }
}

@Composable
private fun CategoryBar(type: String, emi: Double, total: Double, color: Color, fmt: NumberFormat) {
    val ratio = if (total > 0) (emi / total).toFloat().coerceIn(0f, 1f) else 0f
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(type, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Text(fmt.format(emi), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = color)
        }
        Spacer(Modifier.height(4.dp))
        Box(modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(color.copy(alpha = 0.12f))) {
            Box(modifier = Modifier.fillMaxWidth(ratio).fillMaxHeight().clip(RoundedCornerShape(3.dp)).background(color))
        }
    }
}

@Composable
private fun LoanAnalyticsRow(loan: Loan, fmt: NumberFormat) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(Indigo50),
                contentAlignment = Alignment.Center,
            ) {
                Text(loanEmoji(loan.type), fontSize = 18.sp)
            }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(loan.name, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Text(loan.loanType.displayName, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(fmt.format(loan.emiAmount), fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun EmptyAnalytics(modifier: Modifier) {
    Column(modifier = modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Box(modifier = Modifier.size(100.dp).clip(CircleShape).background(Indigo50), contentAlignment = Alignment.Center) {
            Text("📊", fontSize = 44.sp)
        }
        Spacer(Modifier.height(20.dp))
        Text("No Analytics Yet", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.height(8.dp))
        Text("Add loans to see your portfolio analytics and insights.", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 22.sp)
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B), letterSpacing = 0.5.sp)
}

private fun formatLakh(value: Double): String = when {
    value >= 10_00_000 -> "%.1fL".format(value / 1_00_000)
    value >= 1_000 -> "%.0fK".format(value / 1_000)
    else -> "%.0f".format(value)
}

private fun loanTypeColor(type: LoanType): Color = when (type) {
    LoanType.HOME -> HomeLoanColor
    LoanType.CAR -> CarLoanColor
    LoanType.PERSONAL -> PersonalLoanColor
    LoanType.EDUCATION -> Violet600
    LoanType.BUSINESS -> Indigo600
    LoanType.OTHER -> OtherLoanColor
}

private fun loanEmoji(type: String) = when (type.toLoanType()) {
    LoanType.HOME -> "🏠"; LoanType.CAR -> "🚗"; LoanType.PERSONAL -> "👤"
    LoanType.EDUCATION -> "🎓"; LoanType.BUSINESS -> "💼"; LoanType.OTHER -> "💰"
}
