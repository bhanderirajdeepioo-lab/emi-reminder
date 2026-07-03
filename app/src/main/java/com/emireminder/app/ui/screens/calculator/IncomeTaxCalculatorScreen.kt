package com.emireminder.app.ui.screens.calculator

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import java.text.NumberFormat
import java.util.Locale

private val TaxBlue     = Color(0xFF1D4ED8)
private val TaxBlueDark = Color(0xFF1E3A8A)
private val TaxBlue50   = Color(0xFFEFF6FF)
private val TaxBlueLight = Color(0xFFBFDBFE)
private val TaxGreen    = Color(0xFF16A34A)
private val TaxGreen50  = Color(0xFFF0FDF4)
private val TaxAmber50  = Color(0xFFFFFBEB)
private val TaxAmber    = Color(0xFFD97706)

private val _taxFmt = NumberFormat.getNumberInstance(Locale("en", "IN")).apply { maximumFractionDigits = 0 }
private fun fmtTax(v: Double) = "₹${_taxFmt.format(v.toLong())}"

@Composable
fun IncomeTaxCalculatorScreen(
    onBack: () -> Unit,
    viewModel: IncomeTaxCalculatorViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    if (state.showInterstitialAd) {
        InterstitialAdDialog(onDismiss = viewModel::dismissAd)
    }

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
            // 3-tab regime selector
            RegimeToggle(selected = state.regime, onSelect = viewModel::setRegime)

            // Annual Income — always visible
            IncomeInputCard(
                label = "Annual Income (CTC)",
                value = state.annualIncomeText,
                onValueChange = viewModel::setIncome,
            )

            // Other Income — always visible (interest, rental, freelance, etc.)
            IncomeInputCard(
                label = "Other Income",
                value = state.otherIncomeText,
                onValueChange = viewModel::setOtherIncome,
                placeholder = "Interest, rental, etc.",
            )

            // Old regime deductions — animates in for OLD and COMPARE tabs
            AnimatedVisibility(
                visible = state.regime == TaxRegime.OLD || state.regime == TaxRegime.COMPARE,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "OLD REGIME DEDUCTIONS",
                        fontSize = 10.sp, fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF64748B),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        TaxInputCard(
                            label = "Sec 80C",
                            subtext = "PPF + ELSS + LIC  (max ₹1.5L)",
                            value = state.deduction80CText,
                            onValueChange = viewModel::set80C,
                            modifier = Modifier.weight(1f),
                        )
                        TaxInputCard(
                            label = "Sec 80D",
                            subtext = "Health Ins. self + parents  (max ₹1L)",
                            value = state.deduction80DText,
                            onValueChange = viewModel::set80D,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        TaxInputCard(
                            label = "HRA Exemption",
                            subtext = "Actual HRA exempt amount",
                            value = state.hraExemptionText,
                            onValueChange = viewModel::setHra,
                            modifier = Modifier.weight(1f),
                        )
                        TaxInputCard(
                            label = "Other Deductions",
                            subtext = "NPS, LTA, 80G, etc.",
                            value = state.otherDeductionsText,
                            onValueChange = viewModel::setOtherDeductions,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }

            // Standard deduction info — hidden in COMPARE (each regime card carries its own label)
            AnimatedVisibility(
                visible = state.regime != TaxRegime.COMPARE,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(1.dp),
                ) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("Standard Deduction", fontSize = 12.sp, color = Color(0xFF64748B))
                        Text(
                            if (state.regime == TaxRegime.NEW) "₹75,000  (New regime 2024–25)"
                            else "₹50,000  (Old regime)",
                            fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TaxBlue,
                        )
                    }
                }
            }

            // Results
            if (state.regime == TaxRegime.COMPARE) {
                CompareResults(state = state)
            } else {
                SingleRegimeResults(state = state)
            }

            // 87A rebate note — new/compare, income ≤ ₹7L
            if (state.regime != TaxRegime.OLD && state.totalIncome in 1.0..7_00_000.0) {
                Card(
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = TaxGreen50),
                    elevation = CardDefaults.cardElevation(0.dp),
                ) {
                    Text(
                        "✓ Rebate u/s 87A: Income ≤₹7L → Nil tax under New Regime",
                        fontSize = 12.sp, color = TaxGreen,
                        modifier = Modifier.padding(12.dp),
                    )
                }
            }

            // Collapsible slab breakdown
            SlabBreakdown(state = state)

            Button(
                onClick = {},
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = TaxBlue),
            ) {
                Text("Calculate Tax →", fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ────────────────────────────────────────────────────────────────────
// Sub-composables
// ────────────────────────────────────────────────────────────────────

@Composable
private fun RegimeToggle(selected: TaxRegime, onSelect: (TaxRegime) -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(TaxBlueDark)
            .padding(4.dp),
    ) {
        Row {
            listOf(
                TaxRegime.NEW     to "New",
                TaxRegime.OLD     to "Old",
                TaxRegime.COMPARE to "Compare",
            ).forEach { (regime, label) ->
                val isSelected = selected == regime
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) Color.White else Color.Transparent)
                        .clickable { onSelect(regime) }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        label, fontSize = 13.sp, fontWeight = FontWeight.Bold,
                        color = if (isSelected) TaxBlue else TaxBlueLight,
                    )
                }
            }
        }
    }
}

@Composable
private fun IncomeInputCard(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String = "",
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(1.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(label, fontSize = 12.sp, color = Color(0xFF64748B))
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth(),
                textStyle = LocalTextStyle.current.copy(
                    fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF1E293B),
                ),
                prefix = { Text("₹ ", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold) },
                placeholder = if (placeholder.isNotEmpty()) {
                    { Text(placeholder, fontSize = 14.sp, color = Color(0xFFCBD5E1)) }
                } else null,
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
}

@Composable
private fun TaxInputCard(
    label: String,
    subtext: String,
    value: String,
    onValueChange: (String) -> Unit,
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
                    fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TaxBlue,
                ),
                prefix = { Text("₹ ", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TaxBlue) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = TaxBlue,
                    unfocusedBorderColor = Color.Transparent,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                ),
            )
            Text(subtext, fontSize = 10.sp, color = Color(0xFF94A3B8))
        }
    }
}

@Composable
private fun SingleRegimeResults(state: IncomeTaxUiState) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            "TAX LIABILITY",
            fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF64748B),
        )

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
                Text(fmtTax(state.totalTax), fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = TaxBlue)
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(
                "Taxable Income" to fmtTax(state.taxableIncome),
                "Eff. Tax Rate"  to "%.1f%%".format(state.effectiveTaxRate),
                "Monthly TDS"    to fmtTax(state.monthlyTds),
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
                        Text(value, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
                    }
                }
            }
        }
    }
}

@Composable
private fun CompareResults(state: IncomeTaxUiState) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            "REGIME COMPARISON",
            fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF64748B),
        )

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            RegimeResultCard(
                title = "New Regime",
                stdDeduction = "Std: ₹75,000",
                totalTax = state.newTotalTax,
                effectiveRate = state.newEffectiveTaxRate,
                monthlyTds = state.newMonthlyTds,
                modifier = Modifier.weight(1f),
            )
            RegimeResultCard(
                title = "Old Regime",
                stdDeduction = "Std: ₹50,000",
                totalTax = state.oldTotalTax,
                effectiveRate = state.oldEffectiveTaxRate,
                monthlyTds = state.oldMonthlyTds,
                modifier = Modifier.weight(1f),
            )
        }

        // Savings callout
        val savings = state.savings
        if (state.totalIncome > 0 && savings != 0.0) {
            val betterRegime = if (savings > 0) "New Regime" else "Old Regime"
            val savingsAmt = kotlin.math.abs(savings)
            val isNewBetter = savings > 0
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isNewBetter) TaxGreen50 else TaxAmber50
                ),
                elevation = CardDefaults.cardElevation(0.dp),
            ) {
                Text(
                    "💰 $betterRegime saves you ${fmtTax(savingsAmt)}  (${fmtTax(savingsAmt / 12)}/mo)",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isNewBetter) TaxGreen else TaxAmber,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                )
            }
        }
    }
}

@Composable
private fun RegimeResultCard(
    title: String,
    stdDeduction: String,
    totalTax: Double,
    effectiveRate: Double,
    monthlyTds: Double,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        elevation = CardDefaults.cardElevation(2.dp),
    ) {
        Column(
            Modifier.padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(title, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TaxBlueLight)
            Text(stdDeduction, fontSize = 10.sp, color = Color(0xFF64748B))
            Spacer(Modifier.height(2.dp))
            Text(fmtTax(totalTax), fontSize = 19.sp, fontWeight = FontWeight.ExtraBold, color = TaxBlue)
            Text("%.1f%% rate".format(effectiveRate), fontSize = 11.sp, color = Color(0xFF94A3B8))
            Text(fmtTax(monthlyTds) + "/mo", fontSize = 11.sp, color = Color(0xFF94A3B8))
        }
    }
}

@Composable
private fun SlabBreakdown(state: IncomeTaxUiState) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(1.dp),
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Tax Slab Breakdown",
                    fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B),
                )
                Icon(
                    if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null, tint = TaxBlue,
                )
            }

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                Column(Modifier.padding(bottom = 12.dp)) {
                    when (state.regime) {
                        TaxRegime.NEW     -> SlabTable(label = null, slabs = state.newSlabs)
                        TaxRegime.OLD     -> SlabTable(label = null, slabs = state.oldSlabs)
                        TaxRegime.COMPARE -> {
                            SlabTable(label = "New Regime", slabs = state.newSlabs)
                            Spacer(Modifier.height(8.dp))
                            HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                            SlabTable(label = "Old Regime", slabs = state.oldSlabs)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SlabTable(label: String?, slabs: List<SlabRow>) {
    if (label != null) {
        Text(
            label,
            fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
        )
    }
    Row(
        Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
    ) {
        Text("Range", fontSize = 11.sp, color = Color(0xFF94A3B8), modifier = Modifier.weight(2f))
        Text("Rate",  fontSize = 11.sp, color = Color(0xFF94A3B8), modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
        Text("Tax",   fontSize = 11.sp, color = Color(0xFF94A3B8), modifier = Modifier.weight(2f), textAlign = TextAlign.End)
    }
    HorizontalDivider(Modifier.padding(horizontal = 16.dp))
    slabs.forEach { slab ->
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(slab.range, fontSize = 12.sp, color = Color(0xFF1E293B), modifier = Modifier.weight(2f))
            Text(slab.rate,  fontSize = 12.sp, color = Color(0xFF64748B), modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
            Text(
                if (slab.taxOnSlab > 0) fmtTax(slab.taxOnSlab) else "—",
                fontSize = 12.sp,
                fontWeight = if (slab.taxOnSlab > 0) FontWeight.SemiBold else FontWeight.Normal,
                color = if (slab.taxOnSlab > 0) TaxBlue else Color(0xFF94A3B8),
                modifier = Modifier.weight(2f), textAlign = TextAlign.End,
            )
        }
    }
}

@Composable
private fun InterstitialAdDialog(onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.75f))
                .clickable(onClick = onDismiss),
            contentAlignment = Alignment.Center,
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.88f)
                    .clickable(enabled = false) {},
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(8.dp),
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text("Advertisement", fontSize = 10.sp, color = Color(0xFF94A3B8))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(TaxBlue50),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text("💰", fontSize = 48.sp)
                            Text(
                                "Smart Tax Planning",
                                fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TaxBlue,
                            )
                            Text(
                                "Invest now to save more tax!",
                                fontSize = 12.sp, color = Color(0xFF64748B),
                            )
                        }
                    }

                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = TaxBlue),
                    ) {
                        Text("Close Ad", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
