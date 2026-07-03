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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.delay
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

    // Interstitial ad: show once per session, 3s after first non-zero result
    var hasShownAdThisSession by remember { mutableStateOf(false) }
    var showInterstitialAd by remember { mutableStateOf(false) }
    val hasResult = state.hraExemptionAnnual > 0.0 || state.taxableHraAnnual > 0.0

    LaunchedEffect(hasResult) {
        if (hasResult && !hasShownAdThisSession) {
            delay(3_000L)
            showInterstitialAd = true
            hasShownAdThisSession = true
        }
    }

    if (showInterstitialAd) {
        HraInterstitialAdOverlay(onDismiss = { showInterstitialAd = false })
    }

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
                                label,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (selected) HraPurple else HraPurpleLight,
                            )
                        }
                    }
                }
            }

            // Basic Salary + DA in a row
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                HraInputCard(
                    label = "Basic Salary",
                    subtext = "per month",
                    value = state.basicSalaryText,
                    onValueChange = viewModel::setBasicSalary,
                    accentColor = HraPurple,
                    modifier = Modifier.weight(1f),
                )
                HraInputCard(
                    label = "Dearness Allow.",
                    subtext = "per month (DA)",
                    value = state.daText,
                    onValueChange = viewModel::setDearness,
                    accentColor = HraPurple,
                    modifier = Modifier.weight(1f),
                )
            }

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
                    label = "Rent Paid",
                    subtext = "per month",
                    value = state.rentPaidText,
                    onValueChange = viewModel::setRentPaid,
                    accentColor = HraPurple,
                    modifier = Modifier.weight(1f),
                )
            }

            // No-rent note
            val rentPaid = state.rentPaidText.toDoubleOrNull() ?: 0.0
            if (rentPaid == 0.0) {
                Card(
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF7ED)),
                    elevation = CardDefaults.cardElevation(0.dp),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text("⚠", fontSize = 16.sp)
                        Text(
                            "Rent = ₹0 — Rule ③ yields zero, so HRA is fully taxable.",
                            fontSize = 12.sp,
                            color = Color(0xFF92400E),
                        )
                    }
                }
            }

            // Rule explainer
            val metroLabel = if (state.cityType == CityType.METRO) "50% of Basic+DA (Metro)" else "40% of Basic+DA (Non-Metro)"
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(1.dp),
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        "Exemption = least of these 3 rules  [Sec 10(13A)]",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF374151),
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "① Actual HRA received\n② $metroLabel\n③ Rent − 10% of Basic+DA",
                        fontSize = 11.sp,
                        color = Color(0xFF6B7280),
                        lineHeight = 18.sp,
                    )
                }
            }

            // Section label
            Text(
                "EXEMPTION BREAKDOWN — ALL 3 RULES",
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF64748B),
                modifier = Modifier.padding(top = 4.dp),
            )

            // 3-component breakdown — limiting rule starred + bold (WCAG 1.4.1: not color-only)
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(1.dp),
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    val ruleLabels = listOf(
                        "① Actual HRA received",
                        "② ${if (state.cityType == CityType.METRO) "50%" else "40%"} of Basic+DA",
                        "③ Rent − 10% of Basic+DA",
                    )
                    val ruleValues = listOf(
                        state.component1Monthly,
                        state.component2Monthly,
                        state.component3Monthly,
                    )
                    ruleLabels.forEachIndexed { idx, label ->
                        val ruleNum = idx + 1
                        val isLimiting = ruleNum == state.limitingComponent
                        HraRuleRow(
                            label = label,
                            valueMonthly = ruleValues[idx],
                            valueAnnual = ruleValues[idx] * 12,
                            isLimiting = isLimiting,
                        )
                        if (idx < 2) HorizontalDivider(color = Color(0xFFF1F5F9))
                    }
                }
            }

            // Section label
            Text(
                "EXEMPTION SUMMARY",
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF64748B),
            )

            // Hero result — monthly + annual
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF1E293B))
                    .padding(20.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("HRA Exemption", fontSize = 13.sp, color = Color(0xFF94A3B8))
                    Spacer(Modifier.height(4.dp))
                    Text(
                        fmtHra(state.hraExemptionAnnual),
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = HraPurple,
                    )
                    Text(
                        "annual   •   ${fmtHra(state.hraExemptionMonthly)} / month",
                        fontSize = 12.sp,
                        color = Color(0xFF94A3B8),
                    )
                }
            }

            // 3-up breakdown
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    "Taxable HRA\n(Annual)" to fmtHra(state.taxableHraAnnual),
                    "Tax Saved\n(30% slab)" to fmtHra(state.taxSavedAnnual),
                    "Monthly\nSaving" to fmtHra(state.monthlySaving),
                ).forEach { (label, value) ->
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                        elevation = CardDefaults.cardElevation(0.dp),
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Text(label, fontSize = 10.sp, color = Color(0xFF64748B), lineHeight = 14.sp)
                            Spacer(Modifier.height(4.dp))
                            Text(value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HraRuleRow(
    label: String,
    valueMonthly: Double,
    valueAnnual: Double,
    isLimiting: Boolean,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            if (isLimiting) {
                // Starred + bold text (not color-only) per WCAG 1.4.1
                Text(
                    buildAnnotatedString {
                        withStyle(SpanStyle(fontWeight = FontWeight.ExtraBold, color = Color(0xFF1E293B))) {
                            append("★ $label")
                        }
                    },
                    fontSize = 12.sp,
                )
                Text(
                    "← limiting (applied)",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = HraPurple,
                )
            } else {
                Text(label, fontSize = 12.sp, color = Color(0xFF374151))
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                fmtHra(valueMonthly) + "/mo",
                fontSize = 12.sp,
                fontWeight = if (isLimiting) FontWeight.ExtraBold else FontWeight.Normal,
                color = if (isLimiting) Color(0xFF1E293B) else Color(0xFF6B7280),
            )
            Text(
                fmtHra(valueAnnual) + "/yr",
                fontSize = 10.sp,
                color = Color(0xFF9CA3AF),
            )
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
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = accentColor,
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

@Composable
private fun HraInterstitialAdOverlay(onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xCC000000)),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(HraPurple50),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🏠", fontSize = 48.sp)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Maximise your HRA exemption",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E293B),
                            textAlign = TextAlign.Center,
                        )
                        Text(
                            "EMI Reminder Pro — upgrade today",
                            fontSize = 12.sp,
                            color = Color(0xFF64748B),
                            textAlign = TextAlign.Center,
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                    ) {
                        Text("Skip", color = Color(0xFF64748B))
                    }
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = HraPurple),
                    ) {
                        Text("Learn More")
                    }
                }
            }
        }
    }
}
