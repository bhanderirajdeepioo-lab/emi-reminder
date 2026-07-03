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

private val PpfGreen = Color(0xFF16A34A)
private val PpfGreen50 = Color(0xFFF0FDF4)
private val PpfGreenLight = Color(0xFFDCFCE7)

private val _ppfFmt = NumberFormat.getNumberInstance(Locale("en", "IN")).apply { maximumFractionDigits = 0 }
private fun fmtPpf(v: Double) = "₹${_ppfFmt.format(v.roundToLong())}"

@Composable
fun PPFCalculatorScreen(
    onBack: () -> Unit,
    viewModel: PPFCalculatorViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("PPF Calculator", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = PpfGreen,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                ),
            )
        },
        containerColor = PpfGreen50,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Yearly Investment card
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(1.dp),
            ) {
                Column(Modifier.fillMaxWidth().padding(16.dp)) {
                    Text(
                        "Yearly Investment (max ₹1,50,000)",
                        fontSize = 12.sp, color = Color(0xFF64748B),
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        fmtPpf(state.yearlyInvestment.toDouble()),
                        fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF1E293B),
                    )
                    Slider(
                        value = state.yearlyInvestment,
                        onValueChange = viewModel::setYearlyInvestment,
                        valueRange = 500f..1_50_000f,
                        steps = 0,
                        modifier = Modifier.fillMaxWidth(),
                        colors = SliderDefaults.colors(
                            thumbColor = PpfGreen,
                            activeTrackColor = PpfGreen,
                            inactiveTrackColor = PpfGreenLight,
                        ),
                    )
                }
            }

            // Period + Rate row
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(1.dp),
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Investment Period", fontSize = 12.sp, color = Color(0xFF64748B))
                        Spacer(Modifier.height(8.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(PpfGreen50)
                                    .clickable { viewModel.decrementPeriod() },
                                contentAlignment = Alignment.Center,
                            ) {
                                Text("−", fontSize = 18.sp, color = PpfGreen, fontWeight = FontWeight.Bold)
                            }
                            Text(
                                "${state.periodYears} Yrs",
                                fontSize = 18.sp, fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF1E293B),
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.Center,
                            )
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(PpfGreen50)
                                    .clickable { viewModel.incrementPeriod() },
                                contentAlignment = Alignment.Center,
                            ) {
                                Text("+", fontSize = 18.sp, color = PpfGreen, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(1.dp),
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Interest Rate (Govt. Fixed)", fontSize = 12.sp, color = Color(0xFF64748B))
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "7.1%",
                            fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = PpfGreen,
                        )
                        Text("Updated Q1 2026", fontSize = 10.sp, color = Color(0xFF94A3B8))
                    }
                }
            }

            // Extension chips
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(1.dp),
            ) {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    Text("Extension blocks after maturity", fontSize = 12.sp, color = Color(0xFF64748B))
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PpfExtension.entries.forEach { ext ->
                            val selected = state.extension == ext
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(if (selected) PpfGreen else PpfGreenLight)
                                    .clickable { viewModel.setExtension(ext) }
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                            ) {
                                Text(
                                    ext.label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                                    color = if (selected) Color.White else Color(0xFF15803D),
                                )
                            }
                        }
                    }
                }
            }

            // Section label
            Text(
                "MATURITY BREAKDOWN",
                fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF64748B),
                modifier = Modifier.padding(top = 4.dp),
            )

            // Hero result card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF1E293B))
                    .padding(20.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Maturity Value", fontSize = 13.sp, color = Color(0xFF94A3B8))
                    Spacer(Modifier.height(4.dp))
                    Text(
                        fmtPpf(state.maturityValue),
                        fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = PpfGreen,
                    )
                }
            }

            // 3-up breakdown cards
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    "Total Invested" to fmtPpf(state.totalInvested),
                    "Interest Earned" to fmtPpf(state.interestEarned),
                    "Return" to "%.2f×".format(state.returnMultiple),
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
                colors = ButtonDefaults.buttonColors(containerColor = PpfGreen),
            ) {
                Text("Calculate PPF Returns →", fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
