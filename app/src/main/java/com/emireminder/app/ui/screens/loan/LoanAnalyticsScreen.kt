package com.emireminder.app.ui.screens.loan

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.emireminder.app.data.db.entity.Loan
import com.emireminder.app.domain.model.LoanType
import com.emireminder.app.domain.model.toLoanType
import com.emireminder.app.ui.screens.home.HomeViewModel
import com.emireminder.app.ui.theme.*
import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.min
import kotlin.math.pow

private enum class AnalyticsPeriod(val label: String, val months: Int?) {
    SIX_M("6M", 6), ONE_Y("1Y", 12), THREE_Y("3Y", 36), ALL("All", null)
}

@Composable
fun LoanAnalyticsScreen(
    onBack: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val loans by viewModel.activeLoans.collectAsState()
    val fmt = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
    var period by remember { mutableStateOf(AnalyticsPeriod.ONE_Y) }

    val totalEmi = remember(loans) { loans.sumOf { it.emiAmount } }
    val totalPrincipal = remember(loans) { loans.sumOf { it.principalAmount } }

    val totalInterest = remember(loans) {
        loans.sumOf {
            val r = it.interestRate / (12 * 100)
            if (r == 0.0) 0.0 else {
                val emi = (it.principalAmount * r * Math.pow(1 + r, it.tenureMonths.toDouble())) /
                    (Math.pow(1 + r, it.tenureMonths.toDouble()) - 1)
                (emi * it.tenureMonths) - it.principalAmount
            }
        }
    }

    val today = remember { LocalDate.now() }
    val totalPaid = remember(loans) {
        loans.sumOf { loan ->
            val startDate = Instant.ofEpochMilli(loan.startDate).atZone(ZoneId.systemDefault()).toLocalDate()
            val monthsElapsed = ChronoUnit.MONTHS.between(startDate, today).toInt().coerceIn(0, loan.tenureMonths)
            loan.emiAmount * monthsElapsed
        }
    }
    val remainingInterest = remember(loans) {
        loans.sumOf { loan ->
            val r = loan.interestRate / (12 * 100)
            val startDate = Instant.ofEpochMilli(loan.startDate).atZone(ZoneId.systemDefault()).toLocalDate()
            val monthsElapsed = ChronoUnit.MONTHS.between(startDate, today).toInt().coerceIn(0, loan.tenureMonths)
            if (r == 0.0 || monthsElapsed >= loan.tenureMonths) return@sumOf 0.0
            val remainingMonths = loan.tenureMonths - monthsElapsed
            var balance = loan.principalAmount
            repeat(monthsElapsed) {
                val interest = balance * r
                balance -= (loan.emiAmount - interest)
            }
            balance = maxOf(0.0, balance)
            val remainingEmi = if (r > 0) (balance * r * (1 + r).pow(remainingMonths)) / ((1 + r).pow(remainingMonths) - 1) else loan.emiAmount
            (remainingEmi * remainingMonths) - balance
        }
    }

    val byCategory = remember(loans) {
        loans.groupBy { it.type }
            .mapValues { (_, list) -> list.sumOf { it.emiAmount } }
            .entries.sortedByDescending { it.value }
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

                // Donut chart: Principal vs Interest
                item {
                    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                        SectionLabel("PRINCIPAL vs INTEREST")
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
                                DonutChart(
                                    principal = totalPrincipal,
                                    interest = totalInterest,
                                    modifier = Modifier.size(120.dp),
                                )
                                Spacer(Modifier.width(16.dp))
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    LegendItem("Principal", totalPrincipal, totalPrincipal + totalInterest, Indigo600, fmt)
                                    LegendItem("Interest", totalInterest, totalPrincipal + totalInterest, WarnOrange, fmt)
                                    Divider()
                                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                        Text("Total", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        Text(fmt.format(totalPrincipal + totalInterest), fontWeight = FontWeight.Bold, fontSize = 12.sp)
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

                // Foreclosure savings card
                item {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                        SectionLabel("FORECLOSURE SAVINGS")
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
                                    Column {
                                        Text("If you close all loans today", fontSize = 12.sp, color = Color.White.copy(alpha = 0.7f))
                                        Text("Interest savings potential", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    }
                                    Box(
                                        modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(Color(0xFF059669).copy(alpha = 0.25f)).padding(horizontal = 10.dp, vertical = 6.dp),
                                    ) {
                                        Text("SAVE", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF6EE7B7))
                                    }
                                }
                                Divider(color = Color.White.copy(alpha = 0.15f))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Column {
                                        Text("Remaining Interest", fontSize = 11.sp, color = Color.White.copy(alpha = 0.6f))
                                        Text(fmt.format(remainingInterest.toLong()), fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF6EE7B7))
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("EMIs Saved", fontSize = 11.sp, color = Color.White.copy(alpha = 0.6f))
                                        val avgEmiSaved = if (totalEmi > 0) (remainingInterest / totalEmi).toInt() else 0
                                        Text("~$avgEmiSaved months", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFA5B4FC))
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
private fun StackedBarChart(loans: List<Loan>, period: AnalyticsPeriod, colors: List<Color>, modifier: Modifier) {
    val byCategory = loans.groupBy { it.type }.entries.sortedByDescending { it.value.sumOf { l -> l.emiAmount } }
    val barCount = when (period) {
        AnalyticsPeriod.SIX_M -> 6; AnalyticsPeriod.ONE_Y -> 12; AnalyticsPeriod.THREE_Y -> 36; AnalyticsPeriod.ALL -> 12
    }.coerceAtMost(12)
    val maxEmi = loans.sumOf { it.emiAmount }.toFloat().coerceAtLeast(1f)

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height - 16.dp.toPx()
        val barWidth = (w / barCount * 0.6f)
        val gap = (w / barCount * 0.4f)

        for (barIdx in 0 until barCount) {
            val x = gap / 2 + barIdx * (barWidth + gap)
            var yBottom = h
            byCategory.forEachIndexed { catIdx, (_, catLoans) ->
                val catEmi = catLoans.sumOf { it.emiAmount }.toFloat()
                val barH = (h * catEmi / maxEmi)
                val color = colors.getOrElse(catIdx) { Indigo600 }
                drawRect(
                    color = color,
                    topLeft = Offset(x, yBottom - barH),
                    size = Size(barWidth, barH),
                )
                yBottom -= barH
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
private fun DonutChart(principal: Double, interest: Double, modifier: Modifier) {
    val total = principal + interest
    val principalAngle = if (total > 0) (principal / total * 300f).toFloat() else 150f
    val interestAngle = 300f - principalAngle

    Canvas(modifier = modifier) {
        val stroke = Stroke(width = size.minDimension * 0.18f, cap = StrokeCap.Round)
        val inset = stroke.width / 2 + 4.dp.toPx()
        val arcSize = Size(size.width - inset * 2, size.height - inset * 2)
        val topLeft = Offset(inset, inset)

        drawArc(color = Color(0xFFEEF2FF), startAngle = -210f, sweepAngle = 300f, useCenter = false, topLeft = topLeft, size = arcSize, style = stroke)
        drawArc(color = Color(0xFF4F46E5), startAngle = -210f, sweepAngle = principalAngle, useCenter = false, topLeft = topLeft, size = arcSize, style = stroke)
        drawArc(color = Color(0xFFD97706), startAngle = -210f + principalAngle, sweepAngle = interestAngle, useCenter = false, topLeft = topLeft, size = arcSize, style = stroke)
    }
}

@Composable
private fun LegendItem(label: String, value: Double, total: Double, color: Color, fmt: NumberFormat) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(6.dp))
        Column {
            Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(fmt.format(value), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.weight(1f))
        Text("${if (total > 0) "%.0f".format(value / total * 100) else "0"}%", fontSize = 11.sp, color = color, fontWeight = FontWeight.Bold)
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

private fun loanEmoji(type: String) = when (type.toLoanType()) {
    LoanType.HOME -> "🏠"; LoanType.CAR -> "🚗"; LoanType.PERSONAL -> "👤"
    LoanType.EDUCATION -> "🎓"; LoanType.BUSINESS -> "💼"; LoanType.OTHER -> "💰"
}
