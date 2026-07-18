package com.emireminder.app.ui.screens.calculator

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.hilt.navigation.compose.hiltViewModel
import com.emireminder.app.ui.theme.*
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.roundToLong

private val SipGreen = Color(0xFF059669)
private val SipGreen50 = Color(0xFFECFDF5)

private val _sipFmt = NumberFormat.getNumberInstance(Locale("en", "IN")).also { it.maximumFractionDigits = 0 }
private fun fmtSip(amount: Double): String = "₹${_sipFmt.format(amount.roundToLong())}"

private fun fmtCr(amount: Double): String {
    return when {
        amount >= 1_00_00_000 -> "₹%.2fCr".format(amount / 1_00_00_000)
        amount >= 1_00_000    -> "₹%.2fL".format(amount / 1_00_000)
        else                  -> fmtSip(amount)
    }
}

@Composable
fun SIPCalculatorScreen(
    onBack: () -> Unit,
    viewModel: SIPCalculatorViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

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
                    SipTab.entries.forEach { tab ->
                        val selected = tab == state.selectedTab
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (selected) Color.White else Color.Transparent)
                                .clickable { viewModel.selectTab(tab) }
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
                AnimatedContent(
                    targetState = state.selectedTab,
                    transitionSpec = { fadeIn(tween(200)).togetherWith(fadeOut(tween(200))) },
                    label = "sip_tab_body",
                ) { tab ->
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            if (tab == SipTab.LUMPSUM) "Lump Sum Amount" else "Monthly SIP Amount",
                            fontSize = 11.sp, color = Color(0xFF64748B),
                        )
                        Spacer(Modifier.height(4.dp))
                        val displayAmt = if (tab == SipTab.LUMPSUM) state.lumpSumAmount else state.monthlyAmount
                        Text(fmtSip(displayAmt.toDouble()), fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = Slate800)
                        if (tab == SipTab.LUMPSUM) {
                            Slider(
                                value = state.lumpSumAmount,
                                onValueChange = { viewModel.setLumpSumAmount(it) },
                                valueRange = 10_000f..50_00_000f,
                                colors = SliderDefaults.colors(thumbColor = Indigo600, activeTrackColor = Indigo600, inactiveTrackColor = Indigo100),
                            )
                        } else {
                            Slider(
                                value = state.monthlyAmount,
                                onValueChange = { viewModel.setMonthlyAmount(it) },
                                valueRange = 500f..2_00_000f,
                                colors = SliderDefaults.colors(thumbColor = Indigo600, activeTrackColor = Indigo600, inactiveTrackColor = Indigo100),
                            )
                        }
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
                                    .clickable { viewModel.setAnnualRate((state.annualRate - 0.5f).coerceAtLeast(1f)) }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) { Text("−", fontSize = 16.sp, color = Indigo600, fontWeight = FontWeight.Bold) }
                            Text(
                                "%.1f".format(state.annualRate),
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.Center,
                                fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = Slate800,
                            )
                            Box(
                                modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(Indigo50)
                                    .clickable { viewModel.setAnnualRate((state.annualRate + 0.5f).coerceAtMost(30f)) }
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
                                    .clickable { viewModel.setTenureYears((state.tenureYears - 1).coerceAtLeast(1)) }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) { Text("−", fontSize = 16.sp, color = Indigo600, fontWeight = FontWeight.Bold) }
                            Text(
                                "${state.tenureYears} yr",
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.Center,
                                fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Slate800,
                            )
                            Box(
                                modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(Indigo50)
                                    .clickable { viewModel.setTenureYears((state.tenureYears + 1).coerceAtMost(40)) }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) { Text("+", fontSize = 16.sp, color = Indigo600, fontWeight = FontWeight.Bold) }
                        }
                        Text("(${state.totalMonths} mo)", fontSize = 11.sp, color = Color(0xFF94A3B8), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
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
                        Text(fmtCr(state.corpus), fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF34D399))
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Returns", fontSize = 11.sp, color = Color(0xFF64748B))
                        Text(fmtCr(state.returns), fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFA5B4FC))
                        Text("on ${fmtCr(state.invested)} invested", fontSize = 11.sp, color = Color(0xFF94A3B8))
                    }
                }
            }

            // Breakdown cards
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SIPResultCard("Invested", fmtCr(state.invested), Indigo600, Modifier.weight(1f))
                SIPResultCard("Returns", fmtCr(state.returns), SipGreen, Modifier.weight(1f))
                SIPResultCard("Returns %", "${"%.0f".format(state.returnsPercent)}%", Color(0xFF34D399), Modifier.weight(1f))
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
                    WealthGrowthChart(chartPoints = state.chartPoints, modifier = Modifier.fillMaxWidth().height(160.dp))
                    Spacer(Modifier.height(4.dp))
                    // X-axis labels
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        val step = when {
                            state.tenureYears <= 5 -> 1
                            state.tenureYears <= 15 -> 5
                            else -> 10
                        }
                        (1..state.tenureYears step step).forEach { yr ->
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

        drawAreaFill(chartPoints.map { it.first }, maxVal, w, h, pad, greenAlpha)
        drawAreaFill(chartPoints.map { it.second }, maxVal, w, h, pad, indigoAlpha)
        drawGrowthLine(chartPoints.map { it.first }, maxVal, w, h, pad, green, 2.dp.toPx())
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
