package io.helsy.emireminder.ui.screens.calculator

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.helsy.emireminder.ui.theme.*
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.pow
import kotlin.math.roundToLong

private val SipGreen = Color(0xFF059669)
private val SipGreen50 = Color(0xFFECFDF5)

private enum class SipTab(val label: String) { REGULAR("Regular SIP"), STEPUP("Step-up SIP"), LUMPSUM("Lump Sum") }

/** SIP corpus = PMT * ((1+r)^n - 1) / r * (1+r)
 *  r = monthly rate, n = total months */
private fun calcSIPCorpus(monthly: Double, annualRate: Double, months: Int): Double {
    val r = annualRate / 100.0 / 12.0
    if (r == 0.0) return monthly * months
    return monthly * ((1 + r).pow(months) - 1) / r * (1 + r)
}

private fun calcLumpSum(principal: Double, annualRate: Double, years: Int): Double {
    val r = annualRate / 100.0
    return principal * (1 + r).pow(years)
}

/** Step-up SIP: each year the monthly contribution increases by stepUpPercent% */
private fun calcStepUpSIPCorpus(monthly: Double, annualRate: Double, years: Int, stepUpPercent: Double): Double {
    val r = annualRate / 100.0 / 12.0
    var total = 0.0
    var currentMonthly = monthly
    val totalMonths = years * 12
    for (month in 1..totalMonths) {
        if (month > 1 && (month - 1) % 12 == 0) currentMonthly *= (1 + stepUpPercent / 100.0)
        val remainingMonths = totalMonths - month + 1
        total += currentMonthly * (1 + r).pow(remainingMonths)
    }
    return total
}

private fun fmtSip(amount: Double): String {
    val nf = NumberFormat.getNumberInstance(Locale("en", "IN"))
    nf.maximumFractionDigits = 0
    return "₹${nf.format(amount.roundToLong())}"
}

private fun fmtCr(amount: Double): String {
    return when {
        amount >= 1_00_00_000 -> "₹%.2fCr".format(amount / 1_00_00_000)
        amount >= 1_00_000    -> "₹%.2fL".format(amount / 1_00_000)
        else                  -> fmtSip(amount)
    }
}

@Composable
fun SIPCalculatorScreen(onBack: () -> Unit) {
    var selectedTab by remember { mutableStateOf(SipTab.REGULAR) }
    var monthlyAmount by remember { mutableStateOf(10_000f) }
    var lumpSumAmount by remember { mutableStateOf(1_00_000f) }
    var annualRate by remember { mutableStateOf(12f) }
    var tenureYears by remember { mutableStateOf(15) }

    val totalMonths by remember { derivedStateOf { tenureYears * 12 } }
    val corpus by remember { derivedStateOf {
        when (selectedTab) {
            SipTab.REGULAR  -> calcSIPCorpus(monthlyAmount.toDouble(), annualRate.toDouble(), totalMonths)
            SipTab.STEPUP   -> calcStepUpSIPCorpus(monthlyAmount.toDouble(), annualRate.toDouble(), tenureYears, 10.0)
            SipTab.LUMPSUM  -> calcLumpSum(lumpSumAmount.toDouble(), annualRate.toDouble(), tenureYears)
        }
    }}
    val invested by remember { derivedStateOf {
        when (selectedTab) {
            SipTab.REGULAR -> monthlyAmount.toDouble() * totalMonths
            SipTab.STEPUP  -> {
                // Total invested with 10% annual step-up
                var monthly = monthlyAmount.toDouble()
                var total = 0.0
                for (year in 0 until tenureYears) {
                    total += monthly * 12
                    monthly *= 1.10
                }
                total
            }
            SipTab.LUMPSUM -> lumpSumAmount.toDouble()
        }
    }}
    val returns by remember { derivedStateOf { corpus - invested } }
    val returnsPercent by remember { derivedStateOf { if (invested > 0) (returns / invested * 100) else 0.0 } }

    // Chart data: corpus and invested at each year
    val chartPoints by remember { derivedStateOf {
        (1..tenureYears).map { yr ->
            val months = yr * 12
            val c = when (selectedTab) {
                SipTab.REGULAR  -> calcSIPCorpus(monthlyAmount.toDouble(), annualRate.toDouble(), months)
                SipTab.STEPUP   -> calcStepUpSIPCorpus(monthlyAmount.toDouble(), annualRate.toDouble(), yr, 10.0)
                SipTab.LUMPSUM  -> calcLumpSum(lumpSumAmount.toDouble(), annualRate.toDouble(), yr)
            }
            val inv = when (selectedTab) {
                SipTab.REGULAR  -> monthlyAmount.toDouble() * months
                SipTab.STEPUP   -> {
                    var m = monthlyAmount.toDouble(); var t = 0.0
                    for (y in 0 until yr) { t += m * 12; m *= 1.10 }; t
                }
                SipTab.LUMPSUM  -> lumpSumAmount.toDouble()
            }
            Pair(c, inv)
        }
    }}

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("SIP Calculator", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Indigo600,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                ),
            )
        },
        containerColor = Indigo50,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Tabs
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF312E81))
                    .padding(3.dp)
            ) {
                Row {
                    SipTab.values().forEach { tab ->
                        val selected = tab == selectedTab
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (selected) Color.White else Color.Transparent)
                                .clickable { selectedTab = tab }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                tab.label,
                                fontSize = 12.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                color = if (selected) Indigo600 else Color(0xFFA5B4FC),
                            )
                        }
                    }
                }
            }

            // Amount card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        if (selectedTab == SipTab.LUMPSUM) "Lump Sum Amount" else "Monthly SIP Amount",
                        fontSize = 11.sp, color = Color(0xFF64748B),
                    )
                    Spacer(Modifier.height(4.dp))
                    val displayAmt = if (selectedTab == SipTab.LUMPSUM) lumpSumAmount else monthlyAmount
                    Text(fmtSip(displayAmt.toDouble()), fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = Slate800)
                    if (selectedTab == SipTab.LUMPSUM) {
                        Slider(
                            value = lumpSumAmount,
                            onValueChange = { lumpSumAmount = it },
                            valueRange = 10_000f..50_00_000f,
                            colors = SliderDefaults.colors(thumbColor = Indigo600, activeTrackColor = Indigo600, inactiveTrackColor = Indigo100),
                        )
                    } else {
                        Slider(
                            value = monthlyAmount,
                            onValueChange = { monthlyAmount = it },
                            valueRange = 500f..2_00_000f,
                            colors = SliderDefaults.colors(thumbColor = Indigo600, activeTrackColor = Indigo600, inactiveTrackColor = Indigo100),
                        )
                    }
                }
            }

            // Rate + Tenure
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Expected Return (% p.a.)", fontSize = 11.sp, color = Color(0xFF64748B))
                        Spacer(Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(Indigo50)
                                    .clickable { annualRate = (annualRate - 0.5f).coerceAtLeast(1f) }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) { Text("−", fontSize = 16.sp, color = Indigo600, fontWeight = FontWeight.Bold) }
                            Text(
                                "%.1f".format(annualRate),
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.Center,
                                fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = Slate800,
                            )
                            Box(
                                modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(Indigo50)
                                    .clickable { annualRate = (annualRate + 0.5f).coerceAtMost(30f) }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) { Text("+", fontSize = 16.sp, color = Indigo600, fontWeight = FontWeight.Bold) }
                        }
                    }
                }

                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Investment Period", fontSize = 11.sp, color = Color(0xFF64748B))
                        Spacer(Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(Indigo50)
                                    .clickable { tenureYears = (tenureYears - 1).coerceAtLeast(1) }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) { Text("−", fontSize = 16.sp, color = Indigo600, fontWeight = FontWeight.Bold) }
                            Text(
                                "$tenureYears yr",
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.Center,
                                fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Slate800,
                            )
                            Box(
                                modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(Indigo50)
                                    .clickable { tenureYears = (tenureYears + 1).coerceAtMost(40) }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) { Text("+", fontSize = 16.sp, color = Indigo600, fontWeight = FontWeight.Bold) }
                        }
                        Text("(${tenureYears * 12} mo)", fontSize = 11.sp, color = Color(0xFF94A3B8), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                    }
                }
            }

            // Results hero
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Slate800)
                    .padding(20.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Total Wealth Gained", fontSize = 12.sp, color = Color(0xFF94A3B8))
                        Spacer(Modifier.height(8.dp))
                        Text(fmtCr(corpus), fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF34D399))
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Returns", fontSize = 11.sp, color = Color(0xFF64748B))
                        Text(fmtCr(returns), fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFA5B4FC))
                        Text("on ${fmtCr(invested)} invested", fontSize = 11.sp, color = Color(0xFF94A3B8))
                    }
                }
            }

            // Breakdown cards
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SIPResultCard("Invested", fmtCr(invested), Indigo600, Modifier.weight(1f))
                SIPResultCard("Returns", fmtCr(returns), SipGreen, Modifier.weight(1f))
                SIPResultCard("Returns %", "${"%.0f".format(returnsPercent)}%", Color(0xFF34D399), Modifier.weight(1f))
            }

            // Wealth growth chart
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Wealth Growth Projection", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Slate800)
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(12.dp, 2.dp).background(SipGreen))
                            Spacer(Modifier.width(4.dp))
                            Text("Corpus", fontSize = 10.sp, color = Color(0xFF64748B))
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(12.dp, 2.dp).background(Indigo600))
                            Spacer(Modifier.width(4.dp))
                            Text("Invested", fontSize = 10.sp, color = Color(0xFF64748B))
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    WealthGrowthChart(chartPoints = chartPoints, modifier = Modifier.fillMaxWidth().height(160.dp))
                    Spacer(Modifier.height(4.dp))
                    // X-axis labels
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        val step = when {
                            tenureYears <= 5 -> 1
                            tenureYears <= 15 -> 5
                            else -> 10
                        }
                        (1..tenureYears step step).forEach { yr ->
                            Text("Yr$yr", fontSize = 8.sp, color = Color(0xFF94A3B8))
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun SIPResultCard(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
    ) {
        Column(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(label, fontSize = 10.sp, color = Color(0xFF64748B), textAlign = TextAlign.Center)
            Spacer(Modifier.height(8.dp))
            Text(value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = color, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun WealthGrowthChart(chartPoints: List<Pair<Double, Double>>, modifier: Modifier = Modifier) {
    val green = Color(0xFF059669)
    val indigo = Indigo600
    val greenAlpha = Color(0x59059669)
    val indigoAlpha = Color(0x4D4F46E5)

    Canvas(modifier = modifier) {
        if (chartPoints.isEmpty()) return@Canvas
        val maxVal = chartPoints.maxOf { it.first }.coerceAtLeast(1.0)
        val w = size.width
        val h = size.height
        val pad = 8.dp.toPx()

        fun xOf(i: Int) = pad + (i.toFloat() / (chartPoints.size - 1).coerceAtLeast(1)) * (w - 2 * pad)
        fun yOf(v: Double) = h - pad - ((v / maxVal) * (h - 2 * pad)).toFloat()

        // Draw area fills
        drawAreaFill(chartPoints.map { it.first }, maxVal, w, h, pad, greenAlpha)
        drawAreaFill(chartPoints.map { it.second }, maxVal, w, h, pad, indigoAlpha)

        // Draw corpus line (green solid)
        drawGrowthLine(chartPoints.map { it.first }, maxVal, w, h, pad, green, 2.dp.toPx())

        // Draw invested line (indigo dashed-ish, thinner)
        drawGrowthLine(chartPoints.map { it.second }, maxVal, w, h, pad, indigo, 1.5.dp.toPx())
    }
}

private fun DrawScope.drawAreaFill(
    values: List<Double>,
    maxVal: Double,
    w: Float,
    h: Float,
    pad: Float,
    color: Color,
) {
    if (values.size < 2) return
    val path = Path()
    val n = values.size
    fun xOf(i: Int) = pad + (i.toFloat() / (n - 1)) * (w - 2 * pad)
    fun yOf(v: Double) = h - pad - ((v / maxVal) * (h - 2 * pad)).toFloat()

    path.moveTo(xOf(0), h - pad)
    path.lineTo(xOf(0), yOf(values[0]))
    for (i in 1 until n) {
        val cx = (xOf(i - 1) + xOf(i)) / 2
        path.cubicTo(cx, yOf(values[i - 1]), cx, yOf(values[i]), xOf(i), yOf(values[i]))
    }
    path.lineTo(xOf(n - 1), h - pad)
    path.close()
    drawPath(path, color)
}

private fun DrawScope.drawGrowthLine(
    values: List<Double>,
    maxVal: Double,
    w: Float,
    h: Float,
    pad: Float,
    color: Color,
    strokeWidth: Float,
) {
    if (values.size < 2) return
    val n = values.size
    fun xOf(i: Int) = pad + (i.toFloat() / (n - 1)) * (w - 2 * pad)
    fun yOf(v: Double) = h - pad - ((v / maxVal) * (h - 2 * pad)).toFloat()

    for (i in 1 until n) {
        val cx = (xOf(i - 1) + xOf(i)) / 2
        val path = Path()
        path.moveTo(xOf(i - 1), yOf(values[i - 1]))
        path.cubicTo(cx, yOf(values[i - 1]), cx, yOf(values[i]), xOf(i), yOf(values[i]))
        drawPath(path, color = color, style = androidx.compose.ui.graphics.drawscope.Stroke(strokeWidth))
    }
}
