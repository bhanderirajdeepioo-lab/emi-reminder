package io.helsy.emireminder.ui.screens.calculator

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.helsy.emireminder.ui.theme.*
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.pow
import kotlin.math.roundToLong

private val Amber700 = Color(0xFFD97706)
private val Amber50  = Color(0xFFFFFBEB)
private val Amber100 = Color(0xFFFEF3C7)
private val Amber900 = Color(0xFF92400E)

private enum class FdTab { FD, RD }
private enum class CompoundFreq(val label: String, val n: Int) {
    MONTHLY("Monthly", 12),
    QUARTERLY("Quarterly", 4),
    ANNUALLY("Annually", 1),
    AT_MATURITY("At Maturity", 0),
}

/** FD = P*(1+r/n)^(n*t) ; RD = M*((1+r/n)^(n*t)-1)/(r/n)*(1+r/n) */
private fun calcFD(principal: Double, ratePercent: Double, tenureYears: Double, freq: CompoundFreq): Double {
    val r = ratePercent / 100.0
    return if (freq == CompoundFreq.AT_MATURITY) {
        principal * (1 + r * tenureYears)
    } else {
        val n = freq.n.toDouble()
        principal * (1 + r / n).pow(n * tenureYears)
    }
}

private fun calcRD(monthly: Double, ratePercent: Double, tenureYears: Double, freq: CompoundFreq): Double {
    val r = ratePercent / 100.0
    val n = if (freq == CompoundFreq.AT_MATURITY) 1.0 else freq.n.toDouble()
    val periods = n * tenureYears
    val rPerPeriod = r / n
    val monthsPerPeriod = 12.0 / n
    var maturity = 0.0
    val totalMonths = (tenureYears * 12).toInt()
    for (m in 1..totalMonths) {
        val periodsRemaining = (totalMonths - m + 1).toDouble() / monthsPerPeriod
        maturity += monthly * (1 + rPerPeriod).pow(periodsRemaining)
    }
    return maturity
}

private fun fmt(amount: Double): String {
    val nf = NumberFormat.getNumberInstance(Locale("en", "IN"))
    nf.maximumFractionDigits = 0
    return "₹${nf.format(amount.roundToLong())}"
}

@Composable
fun FDRDCalculatorScreen(onBack: () -> Unit) {
    var selectedTab by remember { mutableStateOf(FdTab.FD) }
    var principal by remember { mutableStateOf(100_000f) }
    var monthly by remember { mutableStateOf(10_000f) }
    var ratePercent by remember { mutableStateOf(7.25f) }
    var tenureYears by remember { mutableStateOf(3) }
    var tenureInMonths by remember { mutableStateOf(false) }
    var compoundFreq by remember { mutableStateOf(CompoundFreq.QUARTERLY) }

    val tenureDecimal by remember { derivedStateOf {
        if (tenureInMonths) tenureYears / 12.0 else tenureYears.toDouble()
    }}

    val maturityValue by remember { derivedStateOf {
        if (selectedTab == FdTab.FD)
            calcFD(principal.toDouble(), ratePercent.toDouble(), tenureDecimal, compoundFreq)
        else
            calcRD(monthly.toDouble(), ratePercent.toDouble(), tenureDecimal, compoundFreq)
    }}
    val principalForCalc by remember { derivedStateOf {
        if (selectedTab == FdTab.FD) principal.toDouble() else monthly * (tenureYears * if (tenureInMonths) 1 else 12).toDouble()
    }}
    val interest by remember { derivedStateOf { maturityValue - principalForCalc } }
    val effectiveRate by remember { derivedStateOf {
        if (tenureDecimal > 0) ((maturityValue / principalForCalc - 1) / tenureDecimal * 100) else 0.0
    }}

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("FD / RD Calculator", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Amber700,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                )
            )
        },
        containerColor = Amber50,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // FD / RD tab
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Amber900)
                    .padding(3.dp)
            ) {
                Row {
                    FdTab.values().forEach { tab ->
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
                                text = if (tab == FdTab.FD) "Fixed Deposit (FD)" else "Recurring Deposit (RD)",
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                color = if (selected) Amber700 else Amber100,
                                fontSize = 13.sp,
                            )
                        }
                    }
                }
            }

            // Principal / Monthly deposit card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        if (selectedTab == FdTab.FD) "Deposit Amount" else "Monthly Deposit",
                        fontSize = 11.sp, color = Color(0xFF64748B),
                    )
                    Spacer(Modifier.height(4.dp))
                    val display = if (selectedTab == FdTab.FD) principal else monthly
                    Text(
                        fmt(display.toDouble()),
                        fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = Slate800,
                    )
                    if (selectedTab == FdTab.FD) {
                        Slider(
                            value = principal,
                            onValueChange = { principal = it },
                            valueRange = 10_000f..5_000_000f,
                            colors = SliderDefaults.colors(
                                thumbColor = Amber700,
                                activeTrackColor = Amber700,
                                inactiveTrackColor = Amber100,
                            ),
                        )
                    } else {
                        Slider(
                            value = monthly,
                            onValueChange = { monthly = it },
                            valueRange = 500f..200_000f,
                            colors = SliderDefaults.colors(
                                thumbColor = Amber700,
                                activeTrackColor = Amber700,
                                inactiveTrackColor = Amber100,
                            ),
                        )
                    }
                }
            }

            // Rate + Tenure row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Rate stepper
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Interest Rate (% p.a.)", fontSize = 11.sp, color = Color(0xFF64748B))
                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Amber50)
                                    .clickable { if (ratePercent > 1f) ratePercent = (ratePercent - 0.25f).coerceAtLeast(0.25f) }
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                            ) { Text("−", fontSize = 16.sp, color = Amber700, fontWeight = FontWeight.Bold) }
                            Text(
                                "%.2f".format(ratePercent),
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.Center,
                                fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = Slate800,
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Amber50)
                                    .clickable { ratePercent = (ratePercent + 0.25f).coerceAtMost(20f) }
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                            ) { Text("+", fontSize = 16.sp, color = Amber700, fontWeight = FontWeight.Bold) }
                        }
                    }
                }

                // Tenure stepper
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Tenure", fontSize = 11.sp, color = Color(0xFF64748B))
                        Spacer(Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                if (tenureInMonths) "$tenureYears mo" else "$tenureYears yr",
                                fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Slate800,
                                modifier = Modifier.weight(1f),
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Amber50)
                                .padding(2.dp),
                        ) {
                            listOf("Yr" to false, "Mo" to true).forEach { (label, isMonths) ->
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (tenureInMonths == isMonths) Amber700 else Color.Transparent)
                                        .clickable { tenureInMonths = isMonths }
                                        .padding(vertical = 4.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        label,
                                        fontSize = 10.sp,
                                        color = if (tenureInMonths == isMonths) Color.White else Amber700,
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                            }
                        }
                        Slider(
                            value = tenureYears.toFloat(),
                            onValueChange = { tenureYears = it.toInt().coerceAtLeast(1) },
                            valueRange = 1f..if (tenureInMonths) 120f else 10f,
                            colors = SliderDefaults.colors(thumbColor = Amber700, activeTrackColor = Amber700, inactiveTrackColor = Amber100),
                        )
                    }
                }
            }

            // Compounding frequency
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Compounding Frequency", fontSize = 11.sp, color = Color(0xFF64748B))
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        CompoundFreq.values().forEach { freq ->
                            val selected = freq == compoundFreq
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (selected) Amber700 else Amber100)
                                    .clickable { compoundFreq = freq }
                                    .padding(horizontal = 8.dp, vertical = 5.dp),
                            ) {
                                Text(freq.label, fontSize = 10.sp, fontWeight = FontWeight.Bold,
                                    color = if (selected) Color.White else Amber900)
                            }
                        }
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
                Column {
                    Text("Maturity Value", fontSize = 12.sp, color = Color(0xFF94A3B8))
                    Spacer(Modifier.height(8.dp))
                    Text(fmt(maturityValue), fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                }
            }

            // Result breakdown cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                ResultCard(label = "Principal", value = fmt(principalForCalc), color = Slate800, modifier = Modifier.weight(1f))
                ResultCard(label = "Total Interest", value = fmt(interest), color = Amber700, modifier = Modifier.weight(1f))
                ResultCard(label = "Eff. Rate", value = "${"%.2f".format(effectiveRate)}%", color = SafeGreen, modifier = Modifier.weight(1f))
            }

            // Bank comparison (FD only)
            if (selectedTab == FdTab.FD) {
                val bankRates = remember {
                    listOf(
                        "SBI" to 7.10,
                        "HDFC Bank" to 7.25,
                        "ICICI Bank" to 7.20,
                        "Bajaj Finance" to 8.35,
                    )
                }
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Top Bank FD Rates (${tenureYears}${if (tenureInMonths) " mo" else " yr"})",
                            fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Slate800,
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(4.dp))
                                .background(Amber100)
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                        ) {
                            Text("Bank", fontSize = 10.sp, fontWeight = FontWeight.Bold,
                                color = Amber900, modifier = Modifier.weight(1f))
                            Text("Rate", fontSize = 10.sp, fontWeight = FontWeight.Bold,
                                color = Amber900, modifier = Modifier.width(60.dp))
                            Text("Maturity", fontSize = 10.sp, fontWeight = FontWeight.Bold,
                                color = Amber900, modifier = Modifier.width(90.dp), textAlign = TextAlign.End)
                        }
                        Spacer(Modifier.height(4.dp))
                        val bestIdx = bankRates.indices.maxByOrNull { bankRates[it].second } ?: 0
                        bankRates.forEachIndexed { idx, (name, bankRate) ->
                            val bankMaturity = calcFD(
                                principal.toDouble(), bankRate, tenureDecimal, compoundFreq,
                            )
                            val isBest = idx == bestIdx
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(name, fontSize = 12.sp, modifier = Modifier.weight(1f))
                                if (isBest) {
                                    Box(
                                        modifier = Modifier
                                            .width(60.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Amber700)
                                            .padding(horizontal = 4.dp, vertical = 2.dp),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text("%.2f%% ★".format(bankRate), fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold, color = Color.White)
                                    }
                                } else {
                                    Text("%.2f%%".format(bankRate), fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold, color = SafeGreen,
                                        modifier = Modifier.width(60.dp))
                                }
                                Text(fmt(bankMaturity), fontSize = 12.sp,
                                    modifier = Modifier.width(90.dp), textAlign = TextAlign.End)
                            }
                            if (idx < bankRates.lastIndex)
                                HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun ResultCard(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
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
            Text(value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = color, textAlign = TextAlign.Center)
        }
    }
}
