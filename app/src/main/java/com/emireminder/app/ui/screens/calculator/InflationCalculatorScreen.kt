package com.emireminder.app.ui.screens.calculator

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.delay
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

    var showInterstitialAd by remember { mutableStateOf(false) }
    var hasShownAdThisSession by remember { mutableStateOf(false) }

    LaunchedEffect(state.showResults) {
        if (state.showResults && !hasShownAdThisSession) {
            delay(3_000L)
            showInterstitialAd = true
            hasShownAdThisSession = true
        }
    }

    if (showInterstitialAd) {
        InflationInterstitialAdOverlay(onDismiss = { showInterstitialAd = false })
    }

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
            // Tool header card
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(1.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text("📈", fontSize = 32.sp)
                    Column {
                        Text(
                            "Inflation Calculator",
                            fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B),
                        )
                        Text(
                            "See how inflation affects your money's real value",
                            fontSize = 13.sp, color = Color(0xFF64748B),
                        )
                    }
                }
            }

            // Mode toggle — Segmented Button
            InflationModeToggle(
                selectedMode = state.mode,
                onModeSelect = viewModel::setMode,
            )

            // Input form
            if (state.mode == InflationMode.PURCHASING_POWER) {
                ModeAInputForm(
                    currentAmountText = state.currentAmountText,
                    inflationRateText = state.inflationRateText,
                    yearsText = state.yearsText,
                    selectedScenario = state.selectedScenario,
                    onCurrentAmountChange = viewModel::setCurrentAmountText,
                    onInflationRateChange = viewModel::setInflationRateText,
                    onYearsChange = viewModel::setYearsText,
                    onScenarioSelect = viewModel::setInflationRateFromScenario,
                )
            } else {
                ModeBInputForm(
                    nominalRateText = state.nominalRateText,
                    inflationRateText = state.inflationRateText,
                    selectedScenario = state.selectedScenario,
                    onNominalRateChange = viewModel::setNominalRateText,
                    onInflationRateChange = viewModel::setInflationRateText,
                    onScenarioSelect = viewModel::setInflationRateFromScenario,
                )
            }

            // Calculate CTA
            Button(
                onClick = viewModel::calculate,
                enabled = state.isCalculateEnabled,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = InfOrange,
                    disabledContainerColor = Color(0xFFD1D5DB),
                ),
            ) {
                Text(
                    "Calculate →",
                    fontSize = 15.sp, fontWeight = FontWeight.Bold,
                )
            }

            // Results card (animated in after calculate)
            AnimatedVisibility(
                visible = state.showResults,
                enter = expandVertically(tween(300)) + fadeIn(tween(300)),
                exit = shrinkVertically(tween(300)) + fadeOut(tween(300)),
            ) {
                when (state.mode) {
                    InflationMode.PURCHASING_POWER -> state.resultA?.let { result ->
                        ModeAResultCard(result = result, onReset = viewModel::reset)
                    }
                    InflationMode.REAL_RETURNS -> state.resultB?.let { result ->
                        ModeBResultCard(result = result, onReset = viewModel::reset)
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun InflationModeToggle(
    selectedMode: InflationMode,
    onModeSelect: (InflationMode) -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(InfOrangeDark)
            .padding(4.dp),
    ) {
        Row {
            listOf(
                InflationMode.PURCHASING_POWER to "Purchasing Power",
                InflationMode.REAL_RETURNS to "Real Returns",
            ).forEach { (mode, label) ->
                val isSelected = selectedMode == mode
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) Color.White else Color.Transparent)
                        .clickable { onModeSelect(mode) }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        label,
                        fontSize = 13.sp, fontWeight = FontWeight.Bold,
                        color = if (isSelected) InfOrange else InfOrangeLight,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@Composable
private fun ModeAInputForm(
    currentAmountText: String,
    inflationRateText: String,
    yearsText: String,
    selectedScenario: InflationScenario?,
    onCurrentAmountChange: (String) -> Unit,
    onInflationRateChange: (String) -> Unit,
    onYearsChange: (String) -> Unit,
    onScenarioSelect: (InflationScenario) -> Unit,
) {
    InflationInputField(
        label = "Current Amount (₹)",
        value = currentAmountText,
        onValueChange = onCurrentAmountChange,
        placeholder = "e.g. 1,00,000",
        prefix = "₹",
        keyboardType = KeyboardType.Number,
    )
    InflationInputField(
        label = "Inflation Rate (% p.a.)",
        value = inflationRateText,
        onValueChange = onInflationRateChange,
        placeholder = "6.00",
        suffix = "%",
        keyboardType = KeyboardType.Decimal,
        helperText = "India's avg. CPI",
    )
    ScenarioChipRow(selectedScenario = selectedScenario, onScenarioSelect = onScenarioSelect)
    InflationInputField(
        label = "Time Period (years)",
        value = yearsText,
        onValueChange = onYearsChange,
        placeholder = "1–50",
        suffix = "Yrs",
        keyboardType = KeyboardType.Number,
    )
}

@Composable
private fun ModeBInputForm(
    nominalRateText: String,
    inflationRateText: String,
    selectedScenario: InflationScenario?,
    onNominalRateChange: (String) -> Unit,
    onInflationRateChange: (String) -> Unit,
    onScenarioSelect: (InflationScenario) -> Unit,
) {
    InflationInputField(
        label = "Nominal Return Rate (% p.a.)",
        value = nominalRateText,
        onValueChange = onNominalRateChange,
        placeholder = "e.g. 12.00",
        suffix = "%",
        keyboardType = KeyboardType.Decimal,
        helperText = "Can be negative (e.g. -5%)",
    )
    InflationInputField(
        label = "Inflation Rate (% p.a.)",
        value = inflationRateText,
        onValueChange = onInflationRateChange,
        placeholder = "6.00",
        suffix = "%",
        keyboardType = KeyboardType.Decimal,
        helperText = "India's avg. CPI",
    )
    ScenarioChipRow(selectedScenario = selectedScenario, onScenarioSelect = onScenarioSelect)
}

@Composable
private fun ScenarioChipRow(
    selectedScenario: InflationScenario?,
    onScenarioSelect: (InflationScenario) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        InflationScenario.entries.forEach { scenario ->
            val isSelected = selectedScenario == scenario
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (isSelected) InfOrange else Color.White)
                    .border(
                        width = 1.dp,
                        color = if (isSelected) InfOrange else Color(0xFFE2E8F0),
                        shape = RoundedCornerShape(20.dp),
                    )
                    .clickable { onScenarioSelect(scenario) }
                    .padding(vertical = 6.dp, horizontal = 4.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = scenario.label,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isSelected) Color.White else Color(0xFF64748B),
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun InflationInputField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    prefix: String? = null,
    suffix: String? = null,
    helperText: String? = null,
    keyboardType: KeyboardType = KeyboardType.Number,
) {
    Column(modifier = modifier) {
        Text(label, fontSize = 12.sp, color = Color(0xFF64748B), fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(4.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            placeholder = { Text(placeholder, color = Color(0xFFCBD5E1)) },
            prefix = if (prefix != null) {
                { Text(prefix, fontSize = 16.sp, color = Color(0xFF64748B)) }
            } else null,
            suffix = if (suffix != null) {
                { Text(suffix, fontSize = 14.sp, color = Color(0xFF94A3B8)) }
            } else null,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = InfOrange,
                unfocusedBorderColor = Color(0xFFE2E8F0),
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
            ),
            textStyle = LocalTextStyle.current.copy(
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF1E293B),
            ),
        )
        if (helperText != null) {
            Text(
                helperText,
                fontSize = 11.sp, color = Color(0xFF94A3B8),
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}

@Composable
private fun ModeAResultCard(result: InflationResultA, onReset: () -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                "Purchasing Power Impact",
                fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B),
            )

            HorizontalDivider(color = Color(0xFFE2E8F0))

            // Future cost row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Future Cost (in ${result.years} years)",
                    fontSize = 14.sp, color = Color(0xFF64748B),
                )
                Text(
                    fmtInf(result.futureCost),
                    fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF1E293B),
                )
            }

            HorizontalDivider(color = Color(0xFFE2E8F0))

            // Purchasing power remaining label + arc
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Purchasing Power Remaining", fontSize = 14.sp, color = Color(0xFF64748B))
                Text(
                    "%.2f%%".format(result.purchasingPowerPct),
                    fontSize = 16.sp, fontWeight = FontWeight.Bold, color = InfOrange,
                )
            }

            // Arc visual
            PurchasingPowerArc(
                pct = (result.purchasingPowerPct / 100.0).toFloat(),
                accentColor = InfOrange,
            )

            // Stacked bar chart: retained vs eroded
            PurchasingPowerBar(
                retainedPct = (result.purchasingPowerPct / 100.0).toFloat(),
                retainedAmount = result.purchasingPowerValue,
                erodedAmount = result.purchasingPowerLost,
                accentColor = InfOrange,
            )

            // Purchasing power lost row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Purchasing Power Lost", fontSize = 14.sp, color = Color(0xFF64748B))
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        fmtInf(result.purchasingPowerLost),
                        fontSize = 15.sp, fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error,
                    )
                    Text(
                        "(%.2f%%)".format(result.purchasingPowerLostPct),
                        fontSize = 12.sp, color = MaterialTheme.colorScheme.error,
                    )
                }
            }

            // Summary callout
            Card(
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = InfOrange50),
                elevation = CardDefaults.cardElevation(0.dp),
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text("📌", fontSize = 14.sp)
                    Text(
                        "Your ${fmtInf(result.currentAmount)} today = ${fmtInf(result.purchasingPowerValue)} in " +
                            "real purchasing power after ${result.years} years of " +
                            "${"%.2f".format(result.inflationRate)}% inflation",
                        fontSize = 13.sp, color = InfOrangeDark, lineHeight = 18.sp,
                    )
                }
            }

            // Reset
            OutlinedButton(
                onClick = onReset,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = InfOrange),
                border = ButtonDefaults.outlinedButtonBorder.copy(
                    width = 1.5.dp,
                ),
            ) {
                Text("Reset", fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun ModeBResultCard(result: InflationResultB, onReset: () -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                "Real Return Analysis",
                fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B),
            )

            HorizontalDivider(color = Color(0xFFE2E8F0))

            // Rate rows
            RealReturnRow(
                label = "Nominal Return",
                value = "${"%.2f".format(result.nominalRate)}%",
                color = Color(0xFF1E293B),
            )
            RealReturnRow(
                label = "Inflation Rate",
                value = "${"%.2f".format(result.inflationRate)}%",
                color = Color(0xFF1E293B),
            )

            HorizontalDivider(color = Color(0xFFE2E8F0))

            // Real return — highlighted
            RealReturnRow(
                label = "Real Return (Fisher)",
                value = "${"%.2f".format(result.realReturn)}%",
                color = if (result.beatsInflation) Color(0xFF16A34A) else MaterialTheme.colorScheme.error,
                valueFontSize = 20,
                bold = true,
            )

            // Verdict chip
            if (result.beatsInflation) {
                Card(
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)),
                    elevation = CardDefaults.cardElevation(0.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("✅", fontSize = 16.sp)
                        Text(
                            "Your investment beats inflation by ${"%.2f".format(result.realReturn)}%",
                            fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF166534),
                        )
                    }
                }
            } else {
                Card(
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBEB)),
                    elevation = CardDefaults.cardElevation(0.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("⚠️", fontSize = 16.sp)
                        Text(
                            "Your investment is losing to inflation",
                            fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF92400E),
                        )
                    }
                }
            }

            // Reset
            OutlinedButton(
                onClick = onReset,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = InfOrange),
                border = ButtonDefaults.outlinedButtonBorder.copy(
                    width = 1.5.dp,
                ),
            ) {
                Text("Reset", fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun RealReturnRow(
    label: String,
    value: String,
    color: Color,
    valueFontSize: Int = 15,
    bold: Boolean = false,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, fontSize = 14.sp, color = Color(0xFF64748B))
        Text(
            value,
            fontSize = valueFontSize.sp,
            fontWeight = if (bold) FontWeight.ExtraBold else FontWeight.SemiBold,
            color = color,
        )
    }
}

@Composable
private fun PurchasingPowerBar(
    retainedPct: Float,
    retainedAmount: Double,
    erodedAmount: Double,
    accentColor: Color,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(20.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFFEF4444).copy(alpha = 0.25f)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(retainedPct.coerceIn(0f, 1f))
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(10.dp))
                    .background(accentColor),
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier
                        .size(8.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(accentColor),
                )
                Text(
                    "Retained: ${fmtInf(retainedAmount)}",
                    fontSize = 11.sp, color = accentColor, fontWeight = FontWeight.SemiBold,
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier
                        .size(8.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color(0xFFEF4444)),
                )
                Text(
                    "Eroded: ${fmtInf(erodedAmount)}",
                    fontSize = 11.sp, color = Color(0xFFEF4444), fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
private fun PurchasingPowerArc(pct: Float, accentColor: Color) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val strokeDp = 18.dp
        val radiusDp = (maxWidth - strokeDp) / 2
        // Canvas height shows the full top semicircle including stroke caps
        val canvasHeightDp = radiusDp + strokeDp

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(canvasHeightDp),
        ) {
            val sw = strokeDp.toPx()
            val d = size.width - sw

            // Background arc
            drawArc(
                color = Color(0xFFE2E8F0),
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(sw / 2f, sw / 2f),
                size = Size(d, d),
                style = Stroke(width = sw, cap = StrokeCap.Round),
            )
            // Progress arc
            drawArc(
                color = accentColor,
                startAngle = 180f,
                sweepAngle = 180f * pct.coerceIn(0f, 1f),
                useCenter = false,
                topLeft = Offset(sw / 2f, sw / 2f),
                size = Size(d, d),
                style = Stroke(width = sw, cap = StrokeCap.Round),
            )
        }
    }
}

@Composable
private fun InflationInterstitialAdOverlay(onDismiss: () -> Unit) {
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
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("📢", fontSize = 40.sp)
                Text(
                    "Advertisement",
                    fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B),
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFF1F5F9)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("Ad Placeholder", fontSize = 14.sp, color = Color(0xFF94A3B8))
                }
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = InfOrange),
                ) {
                    Text("Close", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
