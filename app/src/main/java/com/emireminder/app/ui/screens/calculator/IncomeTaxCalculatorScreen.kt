package com.emireminder.app.ui.screens.calculator

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import java.text.NumberFormat
import java.util.Locale

private val TaxBlue = Color(0xFF1D4ED8)
private val TaxBlueDark = Color(0xFF1E3A8A)
private val TaxBlue50 = Color(0xFFEFF6FF)
private val TaxBlueLight = Color(0xFFBFDBFE)

private val _taxFmt = NumberFormat.getNumberInstance(Locale("en", "IN")).apply { maximumFractionDigits = 0 }
private fun fmtTax(v: Double) = "₹${_taxFmt.format(v.toLong())}"

@Composable
fun IncomeTaxCalculatorScreen(
    onBack: () -> Unit,
    viewModel: IncomeTaxCalculatorViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val isNew = state.regime == TaxRegime.NEW

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Income Tax Calculator", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = TaxBlue,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                ),
            )
        },
        containerColor = TaxBlue50,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Regime toggle
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(TaxBlueDark)
                    .padding(4.dp),
            ) {
                Row {
                    listOf(TaxRegime.NEW to "New Regime", TaxRegime.OLD to "Old Regime").forEach { (regime, label) ->
                        val selected = state.regime == regime
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (selected) Color.White else Color.Transparent)
                                .clickable { viewModel.setRegime(regime) }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                label, fontSize = 14.sp, fontWeight = FontWeight.Bold,
                                color = if (selected) TaxBlue else TaxBlueLight,
                            )
                        }
                    }
                }
            }

            // Annual Income
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(1.dp),
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("Annual Income (CTC)", fontSize = 12.sp, color = Color(0xFF64748B))
                    OutlinedTextField(
                        value = state.annualIncomeText,
                        onValueChange = viewModel::setIncome,
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = LocalTextStyle.current.copy(
                            fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF1E293B),
                        ),
                        prefix = { Text("₹ ", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TaxBlue,
                            unfocusedBorderColor = Color.Transparent,
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                        ),
                    )
                }
            }

            // Deductions row (only shown for Old Regime; in New Regime, only standard deduction applies)
            if (!isNew) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TaxInputCard(
                        label = "Sec 80C Deductions",
                        subtext = "PPF + ELSS + LIC",
                        value = state.deduction80CText,
                        onValueChange = viewModel::set80C,
                        accentColor = TaxBlue,
                        modifier = Modifier.weight(1f),
                    )
                    TaxInputCard(
                        label = "Sec 80D Health Ins.",
                        subtext = "Self + parents",
                        value = state.deduction80DText,
                        onValueChange = viewModel::set80D,
                        accentColor = TaxBlue,
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            // Standard deduction info
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(1.dp),
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("Standard Deduction", fontSize = 12.sp, color = Color(0xFF64748B))
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "${fmtTax(state.standardDeduction)}  ${if (isNew) "(New regime 2024–25)" else "(Old regime)"}",
                        fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TaxBlue,
                    )
                }
            }

            // Section label
            Text(
                "TAX LIABILITY",
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
                    Text("Total Tax Payable (incl. 4% cess)", fontSize = 12.sp, color = Color(0xFF94A3B8))
                    Spacer(Modifier.height(4.dp))
                    Text(
                        fmtTax(state.totalTax),
                        fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = TaxBlue,
                    )
                }
            }

            // 3-up breakdown
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    "Taxable Income" to fmtTax(state.taxableIncome),
                    "Eff. Tax Rate" to "%.1f%%".format(state.effectiveTaxRate),
                    "Monthly TDS" to fmtTax(state.monthlyTds),
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

            // Note for new regime rebate
            if (isNew && (state.annualIncomeText.toDoubleOrNull() ?: 0.0) <= 7_00_000) {
                Card(
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF)),
                    elevation = CardDefaults.cardElevation(0.dp),
                ) {
                    Text(
                        "✓ Rebate u/s 87A: Income ≤₹7L → Nil tax under New Regime",
                        fontSize = 12.sp, color = TaxBlue,
                        modifier = Modifier.padding(12.dp),
                    )
                }
            }

            // CTA button
            Button(
                onClick = { /* results are live */ },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = TaxBlue),
            ) {
                Text("Calculate Tax →", fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun TaxInputCard(
    label: String,
    subtext: String,
    value: String,
    onValueChange: (String) -> Unit,
    accentColor: Color,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(1.dp),
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(label, fontSize = 11.sp, color = Color(0xFF64748B))
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth(),
                textStyle = LocalTextStyle.current.copy(
                    fontSize = 16.sp, fontWeight = FontWeight.Bold, color = accentColor,
                ),
                prefix = { Text("₹ ", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = accentColor) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = accentColor,
                    unfocusedBorderColor = Color.Transparent,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                ),
            )
            Text(subtext, fontSize = 10.sp, color = Color(0xFF94A3B8))
        }
    }
}
