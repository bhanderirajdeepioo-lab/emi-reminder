package com.emireminder.app.ui.screens.calculator

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.roundToLong

private val InfOrange = Color(0xFFEA580C)
private val InfOrangeDark = Color(0xFFC2410C)
private val InfOrange50 = Color(0xFFFFF7ED)
private val InfOrangeLight = Color(0xFFFFEDD5)

private val _infFmt = NumberFormat.getNumberInstance(Locale("en", "IN")).apply { maximumFractionDigits = 0 }
private fun fmtInf(v: Double) = "₹${_infFmt.format(v.roundToLong())}"

@Composable
fun InflationCalculatorScreen(
    onBack: () -> Unit,
    viewModel: InflationCalculatorViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Inflation Calculator", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = InfOrange,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                ),
            )
        },
        containerColor = InfOrange50,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Current Amount card
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(1.dp),
            ) {
                Column(Modifier.fillMaxWidth().padding(16.dp)) {
                    Text("Current Amount / Cost Today", fontSize = 12.sp, color = Color(0xFF64748B))
                    Spacer(Modifier.height(4.dp))
                    Text(
                        fmtInf(state.currentAmount.toDouble()),
                        fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF1E293B),
                    )
                    Slider(
                        value = state.currentAmount,
                        onValueChange = viewModel::setCurrentAmount,
                        valueRange = 1_000f..50_00_000f,
                        steps = 0,
                        modifier = Modifier.fillMaxWidth(),
                        colors = SliderDefaults.colors(
                            thumbColor = InfOrange,
                            activeTrackColor = InfOrange,
                            inactiveTrackColor = InfOrangeLight,
                        ),
                    )
                }
            }

            // Rate + Years row
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(1.dp),
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Inflation Rate (% p.a.)", fontSize = 12.sp, color = Color(0xFF64748B))
                        Spacer(Modifier.height(8.dp))
                        InflationStepper(
                            value = "%.1f%%".format(state.inflationRate),
                            onDecrement = viewModel::decrementRate,
                            onIncrement = viewModel::incrementRate,
                            accentColor = InfOrange,
                            bgColor = InfOrange50,
                        )
                    }
                }
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(1.dp),
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Time Period", fontSize = 12.sp, color = Color(0xFF64748B))
                        Spacer(Modifier.height(8.dp))
                        InflationStepper(
                            value = "${state.years} Yrs",
                            onDecrement = viewModel::decrementYears,
                            onIncrement = viewModel::incrementYears,
                            accentColor = InfOrange,
                            bgColor = InfOrange50,
                        )
                    }
                }
            }

            // Quick scenario chips
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(1.dp),
            ) {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    Text("Quick scenarios", fontSize = 12.sp, color = Color(0xFF64748B))
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        InflationScenario.entries.forEach { scenario ->
                            val selected = state.activeScenario == scenario
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(if (selected) InfOrange else InfOrangeLight)
                                    .clickable { viewModel.setScenario(scenario) }
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                            ) {
                                Text(
                                    scenario.label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                                    color = if (selected) Color.White else InfOrangeDark,
                                )
                            }
                        }
                    }
                }
            }

            // Section label
            Text(
                "INFLATION IMPACT",
                fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF64748B),
                modifier = Modifier.padding(top = 4.dp),
            )

            // Hero result
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF1E293B))
                    .padding(20.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "Future Value in ${state.years} Years",
                        fontSize = 13.sp, color = Color(0xFF94A3B8),
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        fmtInf(state.futureValue),
                        fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = InfOrange,
                    )
                }
            }

            // 3-up breakdown
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    "Current Value" to fmtInf(state.currentAmount.toDouble()),
                    "Purchasing Power" to "%.1f%%".format(state.purchasingPowerPct),
                    "Extra Needed" to fmtInf(state.extraNeeded),
                ).forEach { (label, value) ->
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                        elevation = CardDefaults.cardElevation(0.dp),
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Text(label, fontSize = 11.sp, color = Color(0xFF64748B))
                            Spacer(Modifier.height(4.dp))
                            Text(value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
                        }
                    }
                }
            }

            // CTA button
            Button(
                onClick = { /* results are live */ },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = InfOrange),
            ) {
                Text("Calculate Inflation Impact →", fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun InflationStepper(
    value: String,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit,
    accentColor: Color,
    bgColor: Color,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(bgColor)
                .clickable { onDecrement() },
            contentAlignment = Alignment.Center,
        ) {
            Text("−", fontSize = 18.sp, color = accentColor, fontWeight = FontWeight.Bold)
        }
        Text(
            value,
            fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF1E293B),
            modifier = Modifier.weight(1f), textAlign = TextAlign.Center,
        )
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(bgColor)
                .clickable { onIncrement() },
            contentAlignment = Alignment.Center,
        ) {
            Text("+", fontSize = 18.sp, color = accentColor, fontWeight = FontWeight.Bold)
        }
    }
}
