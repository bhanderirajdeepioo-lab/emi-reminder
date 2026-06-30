package io.helsy.emireminder.ui.screens.loan

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import io.helsy.emireminder.data.db.entity.Loan
import io.helsy.emireminder.ui.screens.home.HomeViewModel
import io.helsy.emireminder.ui.theme.*
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.min

@Composable
fun LoanAnalyticsScreen(
    onBack: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val loans by viewModel.activeLoans.collectAsState()
    val fmt = NumberFormat.getCurrencyInstance(Locale("en", "IN"))

    val totalEmi = loans.sumOf { it.emiAmount }
    val totalPrincipal = loans.sumOf { it.principalAmount }

    // Estimated total interest across all loans
    val totalInterest = loans.sumOf {
        val r = it.interestRate / (12 * 100)
        if (r == 0.0) 0.0 else {
            val emi = (it.principalAmount * r * Math.pow(1 + r, it.tenureMonths.toDouble())) /
                (Math.pow(1 + r, it.tenureMonths.toDouble()) - 1)
            (emi * it.tenureMonths) - it.principalAmount
        }
    }

    // Category breakdown
    val byCategory = loans.groupBy { it.type }
        .mapValues { (_, list) -> list.sumOf { it.emiAmount } }
        .entries.sortedByDescending { it.value }

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
                // Summary cards
                item {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        SectionLabel("PORTFOLIO SUMMARY")
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            SummaryCard(modifier = Modifier.weight(1f), label = "Total Loans", value = "${loans.size}", color = Indigo600)
                            SummaryCard(modifier = Modifier.weight(1f), label = "Monthly EMI", value = fmt.format(totalEmi), color = Color(0xFF0891B2))
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            SummaryCard(modifier = Modifier.weight(1f), label = "Total Principal", value = "₹${formatLakh(totalPrincipal)}", color = SafeGreen)
                            SummaryCard(modifier = Modifier.weight(1f), label = "Total Interest", value = "₹${formatLakh(totalInterest)}", color = WarnOrange)
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

                // Loan list
                item {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
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
private fun SummaryCard(modifier: Modifier, label: String, value: String, color: Color) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(6.dp))
            Text(value, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = color)
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
                Text(loan.type.lowercase().replaceFirstChar { it.uppercase() }, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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

private fun loanEmoji(type: String) = when (type.uppercase()) {
    "HOME" -> "🏠"; "CAR" -> "🚗"; "PERSONAL" -> "👤"; "EDUCATION" -> "🎓"; "BUSINESS" -> "💼"; else -> "💰"
}
