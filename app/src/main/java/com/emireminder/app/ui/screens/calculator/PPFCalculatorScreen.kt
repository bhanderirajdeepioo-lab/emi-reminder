package com.emireminder.app.ui.screens.calculator

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.delay
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.roundToLong

private val PpfGreen = Color(0xFF16A34A)
private val PpfGreen50 = Color(0xFFF0FDF4)
private val PpfGreenLight = Color(0xFFDCFCE7)

private val _ppfFmt = NumberFormat.getNumberInstance(Locale("en", "IN")).apply { maximumFractionDigits = 0 }
private fun fmtPpf(v: Double) = "₹${_ppfFmt.format(v.roundToLong())}"
private fun fmtCompact(v: Double): String {
    return when {
        v >= 1_00_00_000 -> "₹${"%.2f".format(v / 1_00_00_000)}Cr"
        v >= 1_00_000 -> "₹${"%.2f".format(v / 1_00_000)}L"
        else -> fmtPpf(v)
    }
}

@Composable
fun PPFCalculatorScreen(
    onBack: () -> Unit,
    viewModel: PPFCalculatorViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    // Fire interstitial ad 3 s after the screen opens, first time per session.
    LaunchedEffect(Unit) {
        if (!state.adShownThisSession) {
            delay(3_000L)
            viewModel.onAdTriggered()
        }
    }

    // Consume the ad trigger — hook real AdMob interstitial show call here.
    LaunchedEffect(state.triggerAd) {
        if (state.triggerAd) {
            // TODO: replace with actual AdMob interstitial call when SDK is integrated
            viewModel.onAdConsumed()
        }
    }

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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Yearly Investment card
            item {
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
            }

            // Period + Rate row
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Investment Period stepper (15 / 20 / 25 … 50 yr)
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

                    // Editable interest rate
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(1.dp),
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Text("Interest Rate (%)", fontSize = 12.sp, color = Color(0xFF64748B))
                            Spacer(Modifier.height(4.dp))
                            OutlinedTextField(
                                value = state.interestRateText,
                                onValueChange = viewModel::setInterestRateText,
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.fillMaxWidth(),
                                textStyle = LocalTextStyle.current.copy(
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = PpfGreen,
                                ),
                                suffix = { Text("%", fontSize = 16.sp, color = Color(0xFF94A3B8)) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = PpfGreen,
                                    unfocusedBorderColor = Color(0xFFE2E8F0),
                                ),
                            )
                            Text("Govt. default: 7.1%", fontSize = 10.sp, color = Color(0xFF94A3B8))
                        }
                    }
                }
            }

            // Extension chips
            item {
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
            }

            // Section label
            item {
                Text(
                    "MATURITY BREAKDOWN",
                    fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF64748B),
                    modifier = Modifier.padding(top = 4.dp),
                )
            }

            // Hero result card
            item {
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
            }

            // 3-up breakdown cards
            item {
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
            }

            // Year-wise breakdown section header (collapsible)
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.toggleBreakdown() },
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(1.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                "Year-wise Accumulation",
                                fontSize = 14.sp, fontWeight = FontWeight.Bold,
                                color = Color(0xFF1E293B),
                            )
                            Text(
                                "${state.periodYears + state.extension.years} years · tap to ${if (state.isBreakdownExpanded) "collapse" else "expand"}",
                                fontSize = 11.sp, color = Color(0xFF64748B),
                            )
                        }
                        Icon(
                            if (state.isBreakdownExpanded) Icons.Default.KeyboardArrowUp
                            else Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            tint = PpfGreen,
                        )
                    }
                }
            }

            // Year-wise breakdown table header row (only when expanded)
            if (state.isBreakdownExpanded) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp))
                            .background(PpfGreen)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                    ) {
                        listOf("Yr", "Deposit", "Interest", "Balance").forEachIndexed { i, col ->
                            Text(
                                col,
                                modifier = Modifier.weight(if (i == 0) 0.5f else 1f),
                                fontSize = 11.sp, fontWeight = FontWeight.Bold,
                                color = Color.White,
                                textAlign = if (i == 0) TextAlign.Start else TextAlign.End,
                            )
                        }
                    }
                }

                // Year data rows — LazyColumn items for performance on 50yr
                items(
                    items = state.yearBreakdowns,
                    key = { it.year },
                ) { row ->
                    val isEven = row.year % 2 == 0
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(if (isEven) Color(0xFFF0FDF4) else Color.White)
                            .padding(horizontal = 12.dp, vertical = 7.dp),
                    ) {
                        Text(
                            "${row.year}",
                            modifier = Modifier.weight(0.5f),
                            fontSize = 12.sp, color = Color(0xFF374151),
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            fmtCompact(row.deposit),
                            modifier = Modifier.weight(1f),
                            fontSize = 12.sp, color = Color(0xFF374151),
                            textAlign = TextAlign.End,
                        )
                        Text(
                            fmtCompact(row.interest),
                            modifier = Modifier.weight(1f),
                            fontSize = 12.sp, color = PpfGreen,
                            textAlign = TextAlign.End,
                        )
                        Text(
                            fmtCompact(row.closingBalance),
                            modifier = Modifier.weight(1f),
                            fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF1E293B),
                            textAlign = TextAlign.End,
                        )
                    }
                    if (row.year == state.yearBreakdowns.last().year) {
                        // Bottom rounded cap
                        Spacer(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(bottomStart = 10.dp, bottomEnd = 10.dp))
                                .background(Color(0xFFE2E8F0)),
                        )
                    }
                }
            }

            // Bottom spacer so last item isn't clipped by nav bar
            item { Spacer(Modifier.height(8.dp)) }
        }
    }
}
