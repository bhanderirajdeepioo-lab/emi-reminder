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
import kotlin.math.roundToLong

private val HraPurple = Color(0xFF7C3AED)
private val HraPurpleDark = Color(0xFF4C1D95)
private val HraPurple50 = Color(0xFFF5F3FF)
private val HraPurpleLight = Color(0xFFEDE9FE)

private val _hraFmt = NumberFormat.getNumberInstance(Locale("en", "IN")).apply { maximumFractionDigits = 0 }
private fun fmtHra(v: Double) = "₹${_hraFmt.format(v.roundToLong())}"

@Composable
fun HRACalculatorScreen(
    onBack: () -> Unit,
    viewModel: HRACalculatorViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("HRA Calculator", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = HraPurple,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                ),
            )
        },
        containerColor = HraPurple50,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Metro / Non-Metro toggle
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(HraPurpleDark)
                    .padding(4.dp),
            ) {
                Row {
                    listOf(CityType.METRO to "Metro City", CityType.NON_METRO to "Non-Metro").forEach { (type, label) ->
                        val selected = state.cityType == type
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (selected) Color.White else Color.Transparent)
                                .clickable { viewModel.setCityType(type) }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                label, fontSize = 14.sp, fontWeight = FontWeight.Bold,
                                color = if (selected) HraPurple else HraPurpleLight,
                            )
                        }
                    }
                }
            }

            // Basic Salary
            HraInputCard(
                label = "Basic Salary (Monthly)",
                value = state.basicSalaryText,
                onValueChange = viewModel::setBasicSalary,
                accentColor = HraPurple,
            )

            // HRA Received + Rent Paid row
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                HraInputCard(
                    label = "HRA Received",
                    subtext = "per month",
                    value = state.hraReceivedText,
                    onValueChange = viewModel::setHraReceived,
                    accentColor = HraPurple,
                    modifier = Modifier.weight(1f),
                )
                HraInputCard(
                    label = "Actual Rent Paid",
                    subtext = "per month",
                    value = state.rentPaidText,
                    onValueChange = viewModel::setRentPaid,
                    accentColor = HraPurple,
                    modifier = Modifier.weight(1f),
                )
            }

            // Rule explainer
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(1.dp),
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        "HRA Exemption is least of these 3 rules:",
                        fontSize = 12.sp, color = Color(0xFF64748B),
                    )
                    Spacer(Modifier.height(6.dp))
                    val metroLabel = if (state.cityType == CityType.METRO) "50% of basic (Metro)" else "40% of basic (Non-Metro)"
                    Text(
                        "① Actual HRA received   ② $metroLabel   ③ Rent − 10% of basic",
                        fontSize = 11.sp, color = Color(0xFF94A3B8),
                    )
                    Spacer(Modifier.height(4.dp))
                    Text("Sec 10(13A) — minimum of all three rules", fontSize = 11.sp, color = HraPurple)
                }
            }

            // Section label
            Text(
                "EXEMPTION SUMMARY",
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
                    Text("HRA Exemption (Annual)", fontSize = 13.sp, color = Color(0xFF94A3B8))
                    Spacer(Modifier.height(4.dp))
                    Text(
                        fmtHra(state.hraExemptionAnnual),
                        fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = HraPurple,
                    )
                }
            }

            // 3-up breakdown
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    "Taxable HRA" to fmtHra(state.taxableHraAnnual),
                    "Tax Saved (30%)" to fmtHra(state.taxSavedAnnual),
                    "Monthly Saving" to fmtHra(state.monthlySaving),
                ).forEach { (label, value) ->
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                        elevation = CardDefaults.cardElevation(0.dp),
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Text(label, fontSize = 10.sp, color = Color(0xFF64748B))
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
                colors = ButtonDefaults.buttonColors(containerColor = HraPurple),
            ) {
                Text("Calculate HRA Exemption →", fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun HraInputCard(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    accentColor: Color,
    subtext: String = "",
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
                    fontSize = 18.sp, fontWeight = FontWeight.Bold, color = accentColor,
                ),
                prefix = { Text("₹ ", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = accentColor) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = accentColor,
                    unfocusedBorderColor = Color.Transparent,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                ),
            )
            if (subtext.isNotBlank()) {
                Text(subtext, fontSize = 10.sp, color = Color(0xFF94A3B8))
            }
        }
    }
}
