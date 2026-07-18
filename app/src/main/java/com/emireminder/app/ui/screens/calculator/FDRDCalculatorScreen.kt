package com.emireminder.app.ui.screens.calculator

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.hilt.navigation.compose.hiltViewModel
import com.emireminder.app.ui.theme.*
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.roundToLong

private val Amber700 = Color(0xFFD97706)
private val Amber50  = Color(0xFFFFFBEB)
private val Amber100 = Color(0xFFFEF3C7)
private val Amber900 = Color(0xFF92400E)

private val _fdFmt = NumberFormat.getNumberInstance(Locale("en", "IN")).also { it.maximumFractionDigits = 0 }
private fun fmt(amount: Double): String = "₹${_fdFmt.format(amount.roundToLong())}"

@Composable
fun FDRDCalculatorScreen(
    onBack: () -> Unit,
    viewModel: FDRDCalculatorViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

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
                    FdTab.entries.forEach { tab ->
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
                AnimatedContent(
                    targetState = state.selectedTab,
                    transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(200)) },
                    label = "fdrd_tab_content",
                ) { tab ->
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            if (tab == FdTab.FD) "Deposit Amount" else "Monthly Deposit",
                            fontSize = 11.sp, color = Color(0xFF64748B),
                        )
                        Spacer(Modifier.height(4.dp))
                        val display = if (tab == FdTab.FD) state.principal else state.monthly
                        Text(
                            fmt(display.toDouble()),
                            fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = Slate800,
                        )
                        if (tab == FdTab.FD) {
                            Slider(
                                value = state.principal,
                                onValueChange = { viewModel.setPrincipal(it) },
                                valueRange = 10_000f..5_000_000f,
                                colors = SliderDefaults.colors(
                                    thumbColor = Amber700,
                                    activeTrackColor = Amber700,
                                    inactiveTrackColor = Amber100,
                                ),
                            )
                        } else {
                            Slider(
                                value = state.monthly,
                                onValueChange = { viewModel.setMonthly(it) },
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
                                    .clickable {
                                        viewModel.setRate((state.ratePercent - 0.25f).coerceAtLeast(0.25f))
                                    }
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                            ) { Text("−", fontSize = 16.sp, color = Amber700, fontWeight = FontWeight.Bold) }
                            Text(
                                "%.2f".format(state.ratePercent),
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.Center,
                                fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = Slate800,
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Amber50)
                                    .clickable {
                                        viewModel.setRate((state.ratePercent + 0.25f).coerceAtMost(20f))
                                    }
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
                                if (state.tenureInMonths) "${state.tenureValue} mo" else "${state.tenureValue} yr",
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
                                        .background(if (state.tenureInMonths == isMonths) Amber700 else Color.Transparent)
                                        .clickable { viewModel.setTenureInMonths(isMonths) }
                                        .padding(vertical = 4.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        label,
                                        fontSize = 10.sp,
                                        color = if (state.tenureInMonths == isMonths) Color.White else Amber700,
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                            }
                        }
                        Slider(
                            value = state.tenureValue.toFloat(),
                            onValueChange = { viewModel.setTenureValue(it.toInt().coerceAtLeast(1)) },
                            valueRange = 1f..if (state.tenureInMonths) 120f else 10f,
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
                        CompoundFreq.entries.forEach { freq ->
                            val selected = freq == state.compoundFreq
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (selected) Amber700 else Amber100)
                                    .clickable { viewModel.setCompoundFreq(freq) }
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
                    Text(fmt(state.maturityValue), fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                }
            }

            // Result breakdown cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                ResultCard(label = "Principal", value = fmt(state.principalForCalc), color = Slate800, modifier = Modifier.weight(1f))
                ResultCard(label = "Total Interest", value = fmt(state.interest), color = Amber700, modifier = Modifier.weight(1f))
                ResultCard(label = "Eff. Rate", value = "${"%.2f".format(state.effectiveRate)}%", color = SafeGreen, modifier = Modifier.weight(1f))
            }

            // Bank comparison (FD only)
            if (state.selectedTab == FdTab.FD) {
                val bestIdx = FD_BANK_RATES.indices.maxByOrNull { FD_BANK_RATES[it].ratePercent } ?: 0
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Bank FD Rate Comparison (${state.tenureValue}${if (state.tenureInMonths) " mo" else " yr"})",
                            fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Slate800,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Tap a row to use that bank's rate",
                            fontSize = 10.sp, color = Color(0xFF94A3B8),
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
                                color = Amber900, modifier = Modifier.width(68.dp))
                            Text("Maturity", fontSize = 10.sp, fontWeight = FontWeight.Bold,
                                color = Amber900, modifier = Modifier.width(90.dp), textAlign = TextAlign.End)
                        }
                        Spacer(Modifier.height(4.dp))
                        FD_BANK_RATES.forEachIndexed { idx, entry ->
                            val bankMaturity = state.bankMaturities.getOrElse(idx) { 0.0 }
                            val isBest = idx == bestIdx
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable { viewModel.setRate(entry.ratePercent.toFloat()) }
                                    .padding(vertical = 6.dp, horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(entry.bankName, fontSize = 12.sp, modifier = Modifier.weight(1f))
                                if (isBest) {
                                    Box(
                                        modifier = Modifier
                                            .width(68.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Amber700)
                                            .padding(horizontal = 4.dp, vertical = 2.dp),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text("%.2f%% ★".format(entry.ratePercent), fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold, color = Color.White)
                                    }
                                } else {
                                    Text("%.2f%%".format(entry.ratePercent), fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold, color = SafeGreen,
                                        modifier = Modifier.width(68.dp))
                                }
                                Text(fmt(bankMaturity), fontSize = 12.sp,
                                    modifier = Modifier.width(90.dp), textAlign = TextAlign.End)
                            }
                            if (idx < FD_BANK_RATES.lastIndex)
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
