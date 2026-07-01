package com.emireminder.app.ui.screens.calculator

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.emireminder.app.ui.theme.*
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.pow

private val PrepayGreenGradient = listOf(Color(0xFF059669), Color(0xFF0891B2))

@Composable
fun PrepaymentCalculatorScreen(
    onBack: () -> Unit,
    viewModel: CalculatorViewModel = hiltViewModel(),
) {
    val activeLoan by viewModel.firstActiveLoan.collectAsStateWithLifecycle()
    val seedPrincipal = activeLoan?.principalAmount?.toLong()?.toString() ?: "1000000"
    val seedRate = activeLoan?.interestRate?.let { "%.2f".format(it) } ?: "10"
    val seedTenure = activeLoan?.tenureMonths?.toString() ?: "120"
    val seedLoanName = activeLoan?.name

    var principal by remember(seedPrincipal) { mutableStateOf(seedPrincipal) }
    var rate by remember(seedRate) { mutableStateOf(seedRate) }
    var tenure by remember(seedTenure) { mutableStateOf(seedTenure) }
    var prepayAmount by remember { mutableStateOf("100000") }
    var prepayMonth by remember { mutableStateOf("12") }
    var goal by remember { mutableStateOf("TENURE") }
    var result by remember { mutableStateOf<PrepayResult?>(null) }
    val fmt = NumberFormat.getCurrencyInstance(Locale("en", "IN"))

    val principalVal = principal.toDoubleOrNull() ?: 1_000_000.0
    val sliderMax = principalVal.toFloat().coerceAtLeast(1f)
    val sliderVal = (prepayAmount.toFloatOrNull() ?: 100_000f).coerceIn(0f, sliderMax)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Prepayment Calculator", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                ),
                modifier = Modifier.background(Brush.horizontalGradient(PrepayGreenGradient)),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (seedLoanName != null) {
                Text(
                    "$seedLoanName — Current loan details pre-filled.",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 22.sp,
                )
            } else {
                Text(
                    "Calculate how much you save by making a lump-sum prepayment on your loan.",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 22.sp,
                )
            }

            SectionLabel("ORIGINAL LOAN DETAILS")
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    InputField("Loan Amount (₹)", principal) { principal = it }
                    InputField("Annual Interest Rate (%)", rate) { rate = it }
                    InputField("Loan Tenure (months)", tenure) { tenure = it }
                }
            }

            SectionLabel("PREPAYMENT DETAILS")
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    InputField("Prepayment Amount (₹)", prepayAmount) { prepayAmount = it }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("₹0", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(fmt.format(principalVal), fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Slider(
                        value = sliderVal,
                        onValueChange = { prepayAmount = it.toLong().toString() },
                        valueRange = 0f..sliderMax,
                        modifier = Modifier.fillMaxWidth(),
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFF059669),
                            activeTrackColor = Color(0xFF059669),
                        ),
                    )
                    InputField("Prepay After Month #", prepayMonth) { prepayMonth = it }
                }
            }

            SectionLabel("PREPAYMENT GOAL")
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                GoalCard(
                    modifier = Modifier.weight(1f),
                    title = "Reduce Tenure",
                    subtitle = "Pay off sooner, same EMI",
                    isSelected = goal == "TENURE",
                    onClick = { goal = "TENURE" },
                )
                GoalCard(
                    modifier = Modifier.weight(1f),
                    title = "Reduce EMI",
                    subtitle = "Lower monthly payment",
                    isSelected = goal == "EMI",
                    onClick = { goal = "EMI" },
                )
            }

            Button(
                onClick = {
                    val p = principal.toDoubleOrNull() ?: return@Button
                    val r = rate.toDoubleOrNull() ?: return@Button
                    val t = tenure.toIntOrNull() ?: return@Button
                    val pa = prepayAmount.toDoubleOrNull() ?: return@Button
                    val pm = prepayMonth.toIntOrNull() ?: return@Button
                    result = calcPrepayment(p, r, t, pa, pm)
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
            ) {
                Icon(Icons.Default.Savings, null)
                Spacer(Modifier.width(8.dp))
                Text("Calculate Savings", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            }

            result?.let { res ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Brush.linearGradient(PrepayGreenGradient))
                        .padding(20.dp),
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Text("Total Savings", fontSize = 13.sp, color = Color.White.copy(alpha = 0.8f))
                        Text(fmt.format(res.totalSavings), fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                        Text(
                            if (goal == "TENURE") "${res.monthsSaved} months off your loan tenure"
                            else "Monthly EMI now ${fmt.format(res.newEmi)}",
                            fontSize = 13.sp,
                            color = Color.White.copy(alpha = 0.85f),
                        )
                    }
                }

                // Sub-metric cards
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SaveMetricCard(Modifier.weight(1f), "New Tenure", "${res.newTenure} mo", Color(0xFF059669))
                    SaveMetricCard(Modifier.weight(1f), "Interest Saved", fmt.format(res.interestWithout - res.interestWith), Color(0xFF0891B2))
                    SaveMetricCard(Modifier.weight(1f), "Months Saved", "${res.monthsSaved}", SafeGreen)
                }

                // Balance comparison chart
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Outstanding Balance", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            LegendDot(Color(0xFF4F46E5), "Without Prepayment")
                            LegendDot(Color(0xFF059669), "With Prepayment")
                        }
                        BalanceComparisonChart(
                            res.balanceSeriesWithout,
                            res.balanceSeriesWith,
                            modifier = Modifier.fillMaxWidth().height(130.dp),
                        )
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Comparison", fontWeight = FontWeight.Bold)
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Text("", modifier = Modifier.weight(1.2f))
                            Text("Without", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("With Prepay", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, fontSize = 12.sp, color = SafeGreen)
                        }
                        Divider()
                        CompareRow("Total Interest", fmt.format(res.interestWithout), fmt.format(res.interestWith))
                        CompareRow("Total Payment", fmt.format(res.totalWithout), fmt.format(res.totalWith))
                        CompareRow("Loan Tenure", "${res.originalTenure} months", "${res.newTenure} months")
                    }
                }
            }
        }
    }
}

@Composable
private fun GoalCard(modifier: Modifier, title: String, subtitle: String, isSelected: Boolean, onClick: () -> Unit) {
    val borderColor = if (isSelected) Color(0xFF059669) else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
    val bgColor = if (isSelected) Color(0xFFECFDF5) else MaterialTheme.colorScheme.surface
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .border(2.dp, borderColor, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 3.dp else 1.dp),
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = if (isSelected) Color(0xFF059669) else MaterialTheme.colorScheme.onSurface,
                )
                if (isSelected) Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF059669), modifier = Modifier.size(16.dp))
            }
            Text(subtitle, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 16.sp)
        }
    }
}

@Composable
private fun SaveMetricCard(modifier: Modifier, label: String, value: String, color: Color) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.08f)),
    ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = color)
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
private fun BalanceComparisonChart(seriesWithout: List<Double>, seriesWith: List<Double>, modifier: Modifier) {
    val indigo = Color(0xFF4F46E5)
    val green = Color(0xFF059669)
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val maxVal = seriesWithout.maxOrNull()?.toFloat() ?: 1f
        val n = seriesWithout.size.coerceAtLeast(2)

        fun xOf(i: Int) = w * i / (n - 1)
        fun yOf(v: Double) = h - (h * (v / maxVal)).toFloat()

        val pathOut = Path().apply {
            seriesWithout.forEachIndexed { i, v -> if (i == 0) moveTo(xOf(i), yOf(v)) else lineTo(xOf(i), yOf(v)) }
        }
        val fillOut = Path().apply {
            addPath(pathOut)
            lineTo(xOf(n - 1), h); lineTo(0f, h); close()
        }
        drawPath(fillOut, indigo.copy(alpha = 0.08f))
        drawPath(pathOut, indigo, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))

        val pathWith = Path().apply {
            seriesWith.forEachIndexed { i, v -> if (i == 0) moveTo(xOf(i), yOf(v)) else lineTo(xOf(i), yOf(v)) }
        }
        val fillWith = Path().apply {
            addPath(pathWith)
            lineTo(xOf(seriesWith.size - 1), h); lineTo(0f, h); close()
        }
        drawPath(fillWith, green.copy(alpha = 0.12f))
        drawPath(pathWith, green, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))
    }
}

private data class PrepayResult(
    val interestWithout: Double,
    val totalWithout: Double,
    val interestWith: Double,
    val totalWith: Double,
    val totalSavings: Double,
    val originalTenure: Int,
    val newTenure: Int,
    val monthsSaved: Int,
    val newEmi: Double,
    val balanceSeriesWithout: List<Double>,
    val balanceSeriesWith: List<Double>,
)

private fun calcPrepayment(
    principal: Double,
    annualRate: Double,
    tenureMonths: Int,
    prepayAmount: Double,
    prepayAtMonth: Int,
): PrepayResult {
    val r = annualRate / (12 * 100)
    val emi = if (r == 0.0) principal / tenureMonths else
        (principal * r * (1 + r).pow(tenureMonths)) / ((1 + r).pow(tenureMonths) - 1)

    val totalWithout = emi * tenureMonths
    val interestWithout = totalWithout - principal

    val seriesWithout = mutableListOf<Double>()
    var bal = principal
    for (m in 1..tenureMonths) {
        val interest = bal * r
        bal -= (emi - interest)
        seriesWithout.add(maxOf(0.0, bal))
    }

    var balance = principal
    var totalPaid = 0.0
    var monthsPaid = 0
    val seriesWith = mutableListOf<Double>()
    var newEmi = emi

    for (month in 1..tenureMonths) {
        if (balance <= 0) break
        val interest = balance * r
        val principalPaid = emi - interest
        balance -= principalPaid
        totalPaid += emi
        monthsPaid++
        if (month == prepayAtMonth && balance > 0) {
            val actualPrepay = minOf(prepayAmount, balance)
            balance -= actualPrepay
            totalPaid += actualPrepay
            val remainingMonths = tenureMonths - month
            if (remainingMonths > 0 && r > 0 && balance > 0) {
                newEmi = (balance * r * (1 + r).pow(remainingMonths)) / ((1 + r).pow(remainingMonths) - 1)
            }
        }
        seriesWith.add(maxOf(0.0, balance))
    }
    while (seriesWith.size < seriesWithout.size) seriesWith.add(0.0)

    val interestWith = totalPaid - principal
    val monthsSaved = tenureMonths - monthsPaid

    return PrepayResult(
        interestWithout = interestWithout,
        totalWithout = totalWithout,
        interestWith = interestWith,
        totalWith = totalPaid,
        totalSavings = totalWithout - totalPaid,
        originalTenure = tenureMonths,
        newTenure = monthsPaid,
        monthsSaved = monthsSaved,
        newEmi = newEmi,
        balanceSeriesWithout = seriesWithout,
        balanceSeriesWith = seriesWith,
    )
}

@Composable
private fun InputField(label: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        singleLine = true,
    )
}

@Composable
private fun CompareRow(label: String, without: String, withPrepay: String) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.weight(1.2f), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(without, modifier = Modifier.weight(1f), fontSize = 12.sp)
        Text(withPrepay, modifier = Modifier.weight(1f), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SafeGreen)
    }
    Divider()
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B), letterSpacing = 0.5.sp)
}
